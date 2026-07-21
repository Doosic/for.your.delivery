package com.foryour.delivery.client.calendar;

import com.foryour.delivery.client.calendar.CalendarPersistenceService.CalendarEventInput;
import com.foryour.delivery.client.calendar.CalendarPersistenceService.CalendarSuggestionInput;
import com.foryour.delivery.client.calendar.CalendarPersistenceService.ProductSnapshot;
import com.foryour.delivery.client.calendar.CalendarPersistenceService.StoredCalendarEvent;
import com.foryour.delivery.client.product.ProductModels.ProductItem;
import com.foryour.delivery.client.product.ProductModels.SearchResponse;
import com.foryour.delivery.client.product.ProductService;
import com.foryour.delivery.common.PersonalDataMasker;
import com.foryour.delivery.domain.entity.CalendarEventEntity;
import com.foryour.delivery.domain.entity.CalendarItemSuggestionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final int MAX_ANALYZED_EVENTS = 20;

  private final OAuth2AuthorizedClientService authorizedClientService;
  private final CalendarPersistenceService calendarPersistenceService;
  private final ProductService productService;
  private final PersonalDataMasker personalDataMasker;

  public CalendarResult suggestions(
      Long userSq,
      String email,
      LocalDate from,
      LocalDate to,
      List<String> calendarIds
  ) {
    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", email);
    if (client == null) {
      return storedOrDemo(userSq, from, to, false, false, "Google Calendar 연결이 필요합니다.");
    }

    try {
      List<CalendarEventInput> fetched = new ArrayList<>();
      for (String calendarId : calendarIds) {
        fetched.addAll(fetchCalendar(client, calendarId, from, to));
      }
      List<CalendarEventEntity> events = calendarPersistenceService.upsertEvents(userSq, fetched);
      Map<String, ProductSelection> productCache = new LinkedHashMap<>();
      int analyzedCount = Math.min(events.size(), MAX_ANALYZED_EVENTS);
      for (int index = 0; index < analyzedCount; index++) {
        CalendarEventEntity event = events.get(index);
        calendarPersistenceService.replaceSuggestions(event, createSuggestions(event, productCache));
      }
      return storedOrDemo(userSq, from, to, true, true, null);
    } catch (Exception error) {
      log.warn("Google Calendar sync failed for {}: {}", email, error.getMessage());
      return storedOrDemo(
          userSq, from, to, true, false,
          "Google Calendar 응답 실패로 마지막 동기화 결과를 표시합니다.");
    }
  }

  public boolean isConnected(String email) {
    return authorizedClientService.loadAuthorizedClient("google", email) != null;
  }

  @SuppressWarnings("unchecked")
  private List<CalendarEventInput> fetchCalendar(
      OAuth2AuthorizedClient client,
      String calendarId,
      LocalDate from,
      LocalDate to
  ) {
    URI uri = UriComponentsBuilder
        .fromUriString("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events")
        .queryParam("timeMin", from.atStartOfDay(SEOUL).toOffsetDateTime().toString())
        .queryParam("timeMax", to.plusDays(1).atStartOfDay(SEOUL).toOffsetDateTime().toString())
        .queryParam("singleEvents", true)
        .queryParam("orderBy", "startTime")
        .queryParam("maxResults", 50)
        .buildAndExpand(calendarId)
        .toUri();
    Map<String, Object> response = RestClient.create()
        .get()
        .uri(uri)
        .header("Authorization", "Bearer " + client.getAccessToken().getTokenValue())
        .retrieve()
        .body(Map.class);
    if (response == null || !(response.get("items") instanceof List<?> items)) {
      return List.of();
    }

    List<CalendarEventInput> events = new ArrayList<>();
    for (Object raw : items) {
      if (!(raw instanceof Map<?, ?> event) || "cancelled".equals(event.get("status"))) {
        continue;
      }
      String externalId = truncate(text(event.get("id"), ""), 255);
      if (!StringUtils.hasText(externalId)) {
        continue;
      }
      Map<String, Object> start = mapValue(event.get("start"));
      Map<String, Object> end = mapValue(event.get("end"));
      EventTime eventTime = eventTime(start, end);
      if (eventTime == null) {
        continue;
      }
      events.add(new CalendarEventInput(
          truncate(calendarId, 255),
          externalId,
          truncate(personalDataMasker.mask(text(event.get("summary"), "제목 없는 일정")), 500),
          personalDataMasker.mask(text(event.get("description"), "")),
          truncate(personalDataMasker.mask(text(event.get("location"), "장소 미정")), 500),
          eventTime.startsAt(),
          eventTime.endsAt(),
          eventTime.allDay()
      ));
    }
    return events;
  }

  private List<CalendarSuggestionInput> createSuggestions(
      CalendarEventEntity event,
      Map<String, ProductSelection> productCache
  ) {
    LocalDate buyBy = event.getStartsAt().toLocalDate().minusDays(2);
    if (buyBy.isBefore(LocalDate.now())) {
      buyBy = LocalDate.now();
    }
    List<CalendarSuggestionInput> suggestions = new ArrayList<>();
    for (PreparationTemplate template : preparationTemplates(
        event.getTitle(), event.getDescription(), event.getLocation())) {
      ProductSelection selection = productCache.get(template.keyword());
      if (selection == null) {
        selection = searchProduct(template.keyword());
        productCache.put(template.keyword(), selection);
      }
      suggestions.add(new CalendarSuggestionInput(
          template.keyword(),
          template.reason(),
          template.priority(),
          buyBy,
          selection.product(),
          selection.live()
      ));
    }
    return suggestions;
  }

  List<PreparationTemplate> preparationTemplates(String title, String description, String location) {
    String context = String.join(" ", safe(title), safe(description), safe(location)).toLowerCase();
    LinkedHashMap<String, PreparationTemplate> templates = new LinkedHashMap<>();
    if (containsAny(context, "캠핑", "야영", "글램핑")) {
      add(templates, "모기퇴치제", "야외 일정에 필요한 방충 준비물", "HIGH");
      add(templates, "캠핑 아이스박스", "식음료 보관이 필요한 야외 일정", "HIGH");
      add(templates, "캠핑 숯", "취사 가능한 캠핑 일정", "MEDIUM");
    }
    if (containsAny(context, "여행", "출장", "휴가", "공항")) {
      add(templates, "여행용 파우치", "이동 일정의 소지품 정리", "MEDIUM");
      add(templates, "보조배터리", "장시간 이동 중 전원 준비", "HIGH");
    }
    if (containsAny(context, "생일", "기념일", "돌잔치", "파티")) {
      add(templates, "선물 포장", "선물 전달 일정 사전 준비", "HIGH");
      add(templates, "생일 카드", "기념 일정 메시지 준비", "MEDIUM");
    }
    if (containsAny(context, "동물병원", "반려묘 병원", "반려견 병원", "펫 병원")) {
      add(templates, "반려동물 이동장 패드", "병원 이동 중 위생 준비", "HIGH");
      add(templates, "반려동물 간식", "진료 전후 안정 보상 준비", "MEDIUM");
    }
    if (containsAny(context, "등산", "트레킹", "러닝", "마라톤")) {
      add(templates, "스포츠 물병", "야외 운동 중 수분 보충", "HIGH");
      add(templates, "자외선 차단제", "야외 활동 피부 보호", "MEDIUM");
    }
    List<PreparationTemplate> limitedTemplates = new ArrayList<>();
    for (PreparationTemplate template : templates.values()) {
      if (limitedTemplates.size() >= 3) {
        break;
      }
      limitedTemplates.add(template);
    }
    return limitedTemplates;
  }

  private ProductSelection searchProduct(String keyword) {
    try {
      SearchResponse response = productService.search(keyword, 3);
      ProductItem item = null;
      for (ProductItem candidate : response.items()) {
        if ("NAVER".equals(candidate.source()) || "ELEVENST".equals(candidate.source())) {
          item = candidate;
          break;
        }
      }
      if (item == null) {
        return new ProductSelection(null, false);
      }
      return new ProductSelection(new ProductSnapshot(
          item.name(), item.source(), item.providerCode(), item.productUrl(), item.imageUrl(), item.price()), true);
    } catch (Exception error) {
      log.warn("Calendar product search failed for {}: {}", keyword, error.getMessage());
      return new ProductSelection(null, false);
    }
  }

  private CalendarResult storedOrDemo(
      Long userSq,
      LocalDate from,
      LocalDate to,
      boolean connected,
      boolean live,
      String warning
  ) {
    List<StoredCalendarEvent> stored = calendarPersistenceService.events(
        userSq, from.atStartOfDay(), to.plusDays(1).atStartOfDay().minusNanos(1));
    if (stored.isEmpty()) {
      if (live) {
        return new CalendarResult(connected, true, List.of(), warning);
      }
      return new CalendarResult(connected, false, demoSuggestions(), warning);
    }
    List<CalendarSuggestion> suggestions = new ArrayList<>();
    for (StoredCalendarEvent event : stored) {
      suggestions.add(toSuggestion(event));
    }
    return new CalendarResult(connected, live, suggestions, warning);
  }

  private CalendarSuggestion toSuggestion(StoredCalendarEvent stored) {
    CalendarEventEntity event = stored.event();
    List<PreparationItem> items = new ArrayList<>();
    for (CalendarItemSuggestionEntity item : stored.suggestions()) {
      items.add(toPreparationItem(item));
    }
    return new CalendarSuggestion(
        event.getCalendarEventSq(),
        event.getExternalEventId(),
        event.getTitle(),
        event.getStartsAt().format(DISPLAY_FORMAT),
        event.getLocation(),
        suggestionText(items),
        items
    );
  }

  private PreparationItem toPreparationItem(CalendarItemSuggestionEntity item) {
    SuggestedProduct product = item.getProvider() == null ? null : new SuggestedProduct(
        item.getProductName(),
        item.getProvider(),
        item.getProviderCode(),
        item.getProductUrl(),
        item.getImageUrl(),
        item.getPrice()
    );
    return new PreparationItem(
        item.getCalendarSuggestionSq(),
        item.getKeyword(),
        item.getReason(),
        item.getRecommendedBuyBy(),
        item.getPriority(),
        Boolean.TRUE.equals(item.getProductLive()),
        product
    );
  }

  private String suggestionText(List<PreparationItem> items) {
    if (items.isEmpty()) {
      return "이 일정에서 별도의 선행구매 준비물이 발견되지 않았습니다.";
    }
    List<String> keywords = new ArrayList<>();
    for (PreparationItem item : items) {
      keywords.add(item.keyword());
    }
    String keywordText = String.join(", ", keywords);
    return keywordText + "은 " + items.getFirst().recommendedBuyBy() + "까지 구매를 권장합니다.";
  }

  private EventTime eventTime(Map<String, Object> start, Map<String, Object> end) {
    try {
      if (start.get("dateTime") != null) {
        LocalDateTime startsAt = OffsetDateTime.parse(start.get("dateTime").toString())
            .atZoneSameInstant(SEOUL).toLocalDateTime();
        LocalDateTime endsAt = end.get("dateTime") == null
            ? startsAt.plusHours(1)
            : OffsetDateTime.parse(end.get("dateTime").toString())
                .atZoneSameInstant(SEOUL).toLocalDateTime();
        return new EventTime(startsAt, endsAt, false);
      }
      if (start.get("date") != null) {
        LocalDate startsOn = LocalDate.parse(start.get("date").toString());
        LocalDate endsOn = end.get("date") == null
            ? startsOn.plusDays(1)
            : LocalDate.parse(end.get("date").toString());
        return new EventTime(startsOn.atStartOfDay(), endsOn.atStartOfDay(), true);
      }
    } catch (RuntimeException error) {
      log.debug("Unsupported Google Calendar event time: {}", error.getMessage());
    }
    return null;
  }

  private List<CalendarSuggestion> demoSuggestions() {
    return List.of(
        demoSuggestion(-1L, "DEMO-CAL-1", "주말 캠핑", "2026-07-25 09:00", "가평", "모기퇴치제", "2026-07-23"),
        demoSuggestion(-2L, "DEMO-CAL-2", "반려묘 병원 방문", "2026-07-28 15:30", "동네 동물병원", "이동장 패드", "2026-07-26"),
        demoSuggestion(-3L, "DEMO-CAL-3", "친구 생일", "2026-08-02 19:00", "성수", "선물 포장", "2026-07-31")
    );
  }

  private CalendarSuggestion demoSuggestion(
      Long eventSq,
      String externalId,
      String title,
      String startsAt,
      String location,
      String keyword,
      String buyBy
  ) {
    LocalDate recommendedBuyBy = LocalDate.parse(buyBy);
    PreparationItem item = new PreparationItem(
        eventSq, keyword, "일정 기반 서버 데모 추천", recommendedBuyBy, "MEDIUM", false, null);
    return new CalendarSuggestion(
        eventSq, externalId, title, startsAt, location,
        keyword + "은 " + buyBy + "까지 구매를 권장합니다.", List.of(item));
  }

  private void add(
      Map<String, PreparationTemplate> templates,
      String keyword,
      String reason,
      String priority
  ) {
    templates.putIfAbsent(keyword, new PreparationTemplate(keyword, reason, priority));
  }

  private boolean containsAny(String context, String... keywords) {
    for (String keyword : keywords) {
      if (context.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mapValue(Object value) {
    return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
  }

  private String text(Object value, String fallback) {
    return value == null || value.toString().isBlank() ? fallback : value.toString();
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }

  private record EventTime(LocalDateTime startsAt, LocalDateTime endsAt, boolean allDay) {
  }

  record PreparationTemplate(String keyword, String reason, String priority) {
  }

  private record ProductSelection(ProductSnapshot product, boolean live) {
  }

  public record SuggestedProduct(
      String name,
      String provider,
      String providerCode,
      String productUrl,
      String imageUrl,
      Long price
  ) {
  }

  public record PreparationItem(
      Long calendarSuggestionSq,
      String keyword,
      String reason,
      LocalDate recommendedBuyBy,
      String priority,
      boolean productLive,
      SuggestedProduct product
  ) {
  }

  public record CalendarSuggestion(
      Long eventSq,
      String calendarEventSq,
      String title,
      String startsAt,
      String location,
      String suggestion,
      List<PreparationItem> items
  ) {
  }

  public record CalendarResult(
      boolean connected,
      boolean live,
      List<CalendarSuggestion> suggestions,
      String warning
  ) {
  }
}
