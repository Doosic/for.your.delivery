package com.foryour.delivery.client.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final OAuth2AuthorizedClientService authorizedClientService;

  public CalendarResult suggestions(String email, LocalDate from, LocalDate to, List<String> calendarIds) {
    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", email);
    if (client == null) {
      return new CalendarResult(false, false, demoSuggestions(), "Google Calendar 연결이 필요합니다.");
    }

    try {
      List<CalendarSuggestion> suggestions = new ArrayList<>();
      for (String calendarId : calendarIds) {
        suggestions.addAll(fetchCalendar(client, calendarId, from, to));
      }
      return new CalendarResult(true, true, suggestions, null);
    } catch (Exception error) {
      log.warn("Google Calendar sync failed for {}: {}", email, error.getMessage());
      return new CalendarResult(true, false, demoSuggestions(), "Google Calendar 응답 실패로 데모 일정을 표시합니다.");
    }
  }

  public boolean isConnected(String email) {
    return authorizedClientService.loadAuthorizedClient("google", email) != null;
  }

  @SuppressWarnings("unchecked")
  private List<CalendarSuggestion> fetchCalendar(
      OAuth2AuthorizedClient client,
      String calendarId,
      LocalDate from,
      LocalDate to
  ) {
    Map<String, Object> response = RestClient.create("https://www.googleapis.com")
        .get()
        .uri(builder -> builder
            .path("/calendar/v3/calendars/{calendarId}/events")
            .queryParam("timeMin", from.atStartOfDay(SEOUL).toOffsetDateTime().toString())
            .queryParam("timeMax", to.plusDays(1).atStartOfDay(SEOUL).toOffsetDateTime().toString())
            .queryParam("singleEvents", true)
            .queryParam("orderBy", "startTime")
            .build(calendarId))
        .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
        .retrieve()
        .body(Map.class);
    if (response == null || !(response.get("items") instanceof List<?> items)) return List.of();

    List<CalendarSuggestion> suggestions = new ArrayList<>();
    for (Object raw : items) {
      if (!(raw instanceof Map<?, ?> event)) continue;
      String title = text(event.get("summary"), "제목 없는 일정");
      String location = text(event.get("location"), "장소 미정");
      Map<String, Object> start = event.get("start") instanceof Map<?, ?> value
          ? (Map<String, Object>) value
          : Map.of();
      String startsAt = formatStart(start);
      suggestions.add(new CalendarSuggestion(
          text(event.get("id"), String.valueOf(suggestions.size() + 1)),
          title,
          startsAt,
          location,
          recommendationFor(title)
      ));
    }
    return suggestions;
  }

  private String formatStart(Map<String, Object> start) {
    Object dateTime = start.get("dateTime");
    if (dateTime != null) {
      return OffsetDateTime.parse(dateTime.toString()).atZoneSameInstant(SEOUL).format(DISPLAY_FORMAT);
    }
    Object date = start.get("date");
    return date == null ? "시간 미정" : date + " 종일";
  }

  private String recommendationFor(String title) {
    String normalized = title.toLowerCase();
    if (normalized.contains("캠핑") || normalized.contains("여행")) return "이동 및 야외 준비물을 일정 2일 전까지 확인하세요.";
    if (normalized.contains("생일") || normalized.contains("기념")) return "선물 후보와 배송 마감일을 미리 확인하세요.";
    if (normalized.contains("병원")) return "이동용품과 필요한 소모품 재고를 확인하세요.";
    return "일정에 필요한 물품이 있는지 재고와 구매목록을 확인하세요.";
  }

  private List<CalendarSuggestion> demoSuggestions() {
    return List.of(
        new CalendarSuggestion("DEMO-CAL-1", "주말 캠핑", "2026-07-25 09:00", "가평", "모기퇴치제, 아이스박스, 숯을 이틀 전까지 확인하세요."),
        new CalendarSuggestion("DEMO-CAL-2", "반려묘 병원 방문", "2026-07-28 15:30", "동네 동물병원", "이동장 패드와 간식 재고를 확인하세요."),
        new CalendarSuggestion("DEMO-CAL-3", "친구 생일", "2026-08-02 19:00", "성수", "선물 후보와 배송 마감일을 미리 확인하세요.")
    );
  }

  private String text(Object value, String fallback) {
    return value == null || value.toString().isBlank() ? fallback : value.toString();
  }

  public record CalendarSuggestion(
      String calendarEventSq,
      String title,
      String startsAt,
      String location,
      String suggestion
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
