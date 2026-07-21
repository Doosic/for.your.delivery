package com.foryour.delivery.client.agent;

import com.foryour.delivery.client.calendar.GoogleCalendarService;
import com.foryour.delivery.client.calendar.GoogleCalendarService.CalendarSuggestion;
import com.foryour.delivery.client.calendar.GoogleCalendarService.PreparationItem;
import com.foryour.delivery.client.calendar.GoogleCalendarService.SuggestedProduct;
import com.foryour.delivery.client.product.ProductModels.HomeFeedResponse;
import com.foryour.delivery.client.product.ProductModels.ImageSources;
import com.foryour.delivery.client.product.ProductModels.ProductFeedItem;
import com.foryour.delivery.client.product.ProductModels.ProductItem;
import com.foryour.delivery.client.product.ProductModels.SearchResponse;
import com.foryour.delivery.client.product.ProductService;
import com.foryour.delivery.domain.entity.ProductOfferEntity;
import com.foryour.delivery.domain.entity.ProductPriceHistoryEntity;
import com.foryour.delivery.domain.enums.AgentTypeCode;
import com.foryour.delivery.domain.repository.ProductOfferRepository;
import com.foryour.delivery.domain.repository.ProductPriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AgentBriefingHarness {

  private static final int HISTORY_DAYS = 30;
  private static final int MIN_FORECAST_SAMPLES = 3;
  private static final int MAX_FORECAST_DAYS = 14;
  private static final Pattern QUERY_TOKEN = Pattern.compile("[가-힣A-Za-z0-9]{2,}");
  private static final Set<String> GENERIC_QUERY_WORDS = Set.of(
      "다음", "일정", "준비물", "언제", "주문", "최저가", "가격", "상품", "추천",
      "구매", "필요", "알려줘", "브리핑", "최근", "조만간", "예정일", "분석"
  );
  private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("M월 d일");

  private final ProductService productService;
  private final GoogleCalendarService googleCalendarService;
  private final ProductOfferRepository productOfferRepository;
  private final ProductPriceHistoryRepository productPriceHistoryRepository;

  public BriefingView today(Long userSq) {
    LocalDate today = LocalDate.now();
    HomeFeedResponse feed = productService.homeFeed(userSq);
    List<CalendarSuggestion> calendar = googleCalendarService
        .storedSuggestions(userSq, today, today.plusDays(30))
        .suggestions();

    List<BriefingItem> calendarItems = calendarItems(calendar, today);
    List<BriefingItem> priceCandidates = priceItems(feed.bestPriceDeals(), today);
    Set<String> calendarProductIds = new HashSet<>();
    for (BriefingItem item : calendarItems) {
      calendarProductIds.add(item.productSq());
    }

    List<BriefingItem> historicalLowItems = new ArrayList<>();
    List<BriefingItem> timingItems = new ArrayList<>();
    for (BriefingItem item : priceCandidates) {
      if (calendarProductIds.contains(item.productSq())) {
        continue;
      }
      if (item.priceInsight().historicalLow() || item.priceInsight().nearHistoricalLow()) {
        if (historicalLowItems.size() < 3) {
          historicalLowItems.add(item);
        }
      } else if (("WAIT".equals(item.decision()) || "PLAN".equals(item.decision()))
          && timingItems.size() < 3) {
        timingItems.add(item);
      }
    }

    List<BriefingSection> sections = new ArrayList<>();
    sections.add(new BriefingSection(
        "CALENDAR",
        "내 일정 준비",
        "앞으로 30일 일정에서 미리 살 품목",
        limit(calendarItems, 4)
    ));
    sections.add(new BriefingSection(
        "HISTORICAL_LOW",
        "지금 가격이 좋은 상품",
        "최근 30일 최저가 또는 최저가 3% 이내",
        historicalLowItems
    ));
    sections.add(new BriefingSection(
        "PRICE_TIMING",
        "조금 기다려볼 상품",
        "최근 가격 하락 추세의 다음 확인일 또는 분석 데이터 수집 상태",
        timingItems
    ));

    int itemCount = calendarItems.size() + historicalLowItems.size() + timingItems.size();
    String summary = itemCount == 0
        ? "일정 준비물과 가격 이력을 분석하고 있어요. 가격 표본이 쌓이면 구매 시점을 안내할게요."
        : "내 일정과 최근 30일 가격을 함께 비교해 지금 살 상품과 기다릴 상품을 나눴어요.";
    String dataMode = feed.live() ? "LIVE" : calendar.isEmpty() ? "COLLECTING" : "PERSISTED";

    return new BriefingView(
        summary,
        sections,
        itemCount > 0,
        dataMode,
        feed.recommendationBasis(),
        feed.interestKeywords(),
        HISTORY_DAYS,
        LocalDateTime.now()
    );
  }

  public AgentContext contextFor(Long userSq, AgentTypeCode route) {
    if (!List.of(
        AgentTypeCode.BRIEFING_SHOPPING,
        AgentTypeCode.CALENDAR_PREPARATION,
        AgentTypeCode.PRICE_INTELLIGENCE
    ).contains(route)) {
      return AgentContext.empty();
    }

    BriefingView briefing = today(userSq);
    List<BriefingItem> recommendations = new ArrayList<>();
    for (BriefingSection section : briefing.sections()) {
      boolean include = AgentTypeCode.BRIEFING_SHOPPING.equals(route)
          || AgentTypeCode.CALENDAR_PREPARATION.equals(route) && "CALENDAR".equals(section.key())
          || AgentTypeCode.PRICE_INTELLIGENCE.equals(route) && !"CALENDAR".equals(section.key());
      if (include) {
        recommendations.addAll(section.items());
      }
    }
    return new AgentContext(
        briefing.summary(), briefing.dataMode(), recommendations, briefing,
        Map.of(
            "priceWindowDays", HISTORY_DAYS,
            "forecastPolicy", "LINEAR_TREND_WITH_DEADLINE_CAP",
            "forecastRequiresSamples", MIN_FORECAST_SAMPLES
        )
    );
  }

  public AgentContext contextFor(Long userSq, AgentTypeCode route, String query) {
    AgentContext context = contextFor(userSq, route);
    List<BriefingItem> focused = focusRecommendations(context.recommendations(), query);
    boolean targetedSearch = false;
    String productQuery = productQuery(query);
    if (focused.isEmpty() && List.of(
        AgentTypeCode.BRIEFING_SHOPPING,
        AgentTypeCode.PRICE_INTELLIGENCE
    ).contains(route)) {
      SearchResponse search = productService.searchForUser(userSq, productQuery, 6, "LOW_PRICE");
      focused = searchItems(search == null ? List.of() : search.items(), LocalDate.now());
      targetedSearch = !focused.isEmpty();
    }
    Map<String, Object> evidencePolicy = new LinkedHashMap<>(context.evidencePolicy());
    if (context.briefing() != null) {
      evidencePolicy.put("recommendationBasis", context.briefing().recommendationBasis());
      evidencePolicy.put("interestKeywords", context.briefing().interestKeywords());
    }
    evidencePolicy.put("contextSources", List.of(
        "PURCHASE_CLICK_DB",
        "PERSONAL_WIKI_DB",
        "CALENDAR_DB",
        "PRODUCT_OFFER_DB",
        "PRICE_HISTORY_DB"
    ));
    evidencePolicy.put("selectionMode", targetedSearch ? "QUERY_PRODUCT_SEARCH" : "PERSONALIZED_DB_CONTEXT");
    if (targetedSearch) {
      evidencePolicy.put("productQuery", productQuery);
    }
    return new AgentContext(
        context.summary(), targetedSearch ? "LIVE_OR_PERSISTED" : context.dataMode(), focused,
        context.briefing(), Map.copyOf(evidencePolicy));
  }

  private List<BriefingItem> focusRecommendations(List<BriefingItem> items, String query) {
    if (items.isEmpty() || query == null || query.isBlank()) {
      return items;
    }
    Set<String> tokens = queryTokens(query);
    if (tokens.isEmpty()) {
      return items;
    }

    List<BriefingItem> focused = new ArrayList<>();
    int bestScore = 0;
    for (BriefingItem item : items) {
      String searchable = String.join(" ",
          value(item.name()), value(item.eventTitle()), value(item.note())).toLowerCase(Locale.ROOT);
      int score = 0;
      for (String token : tokens) {
        if (searchable.contains(token)) {
          score++;
        }
      }
      if (score > bestScore) {
        bestScore = score;
        focused.clear();
        focused.add(item);
      } else if (score > 0 && score == bestScore) {
        focused.add(item);
      }
    }
    return focused;
  }

  private Set<String> queryTokens(String query) {
    Set<String> tokens = new LinkedHashSet<>();
    if (query == null) {
      return tokens;
    }
    Matcher matcher = QUERY_TOKEN.matcher(query.toLowerCase(Locale.ROOT));
    while (matcher.find()) {
      String token = matcher.group();
      if (!isGenericQueryToken(token)) {
        tokens.add(token);
      }
    }
    return tokens;
  }

  private boolean isGenericQueryToken(String token) {
    if (GENERIC_QUERY_WORDS.contains(token)) {
      return true;
    }
    return containsAny(token,
        "주문", "구매", "추천", "가격", "최저", "할인", "상품", "제품", "쇼핑",
        "알려", "보여", "찾아", "골라", "사야", "살까", "사고", "필요", "준비",
        "싶어", "할까", "해줘", "배송", "도착");
  }

  private boolean containsAny(String value, String... candidates) {
    for (String candidate : candidates) {
      if (value.contains(candidate)) {
        return true;
      }
    }
    return false;
  }

  private String productQuery(String query) {
    return String.join(" ", queryTokens(query));
  }

  private String value(String value) {
    return value == null ? "" : value;
  }

  private List<BriefingItem> calendarItems(List<CalendarSuggestion> events, LocalDate today) {
    List<BriefingItem> result = new ArrayList<>();
    for (CalendarSuggestion event : events) {
      for (PreparationItem preparation : event.items()) {
        SuggestedProduct product = preparation.product();
        if (product == null || product.price() == null || product.price() <= 0) {
          continue;
        }
        PriceInsight insight = priceInsight(
            product.provider(), product.providerCode(), product.price(), preparation.recommendedBuyBy(), today);
        PurchaseDecision decision = calendarDecision(preparation.recommendedBuyBy(), insight, today);
        String note = event.title() + " · " + preparation.reason();
        result.add(new BriefingItem(
            productId(product.provider(), product.providerCode()),
            product.name(),
            product.price(),
            providerName(product.provider()),
            product.provider(),
            product.providerCode(),
            product.imageUrl(),
            product.productUrl(),
            null,
            "CALENDAR",
            event.title(),
            event.startsAt(),
            preparation.recommendedBuyBy(),
            decision.code(),
            decision.label(),
            note,
            insight
        ));
      }
    }
    return result;
  }

  private List<BriefingItem> priceItems(List<ProductFeedItem> products, LocalDate today) {
    LinkedHashMap<String, BriefingItem> result = new LinkedHashMap<>();
    for (ProductFeedItem product : products) {
      PriceInsight insight = priceInsight(
          product.source(), product.providerCode(), product.price(), null, today);
      PurchaseDecision decision = priceDecision(insight);
      String note = priceNote(insight);
      result.putIfAbsent(product.productSq(), new BriefingItem(
          product.productSq(),
          product.name(),
          product.price(),
          product.mallName(),
          product.source(),
          product.providerCode(),
          product.imageUrl(),
          product.productUrl(),
          product.imageSources(),
          "PRICE",
          null,
          null,
          null,
          decision.code(),
          decision.label(),
          note,
          insight
      ));
    }
    return new ArrayList<>(result.values());
  }

  private List<BriefingItem> searchItems(List<ProductItem> products, LocalDate today) {
    List<BriefingItem> result = new ArrayList<>();
    for (ProductItem product : products) {
      if (product == null || product.price() <= 0 || result.size() >= 6) {
        continue;
      }
      PriceInsight insight = priceInsight(
          product.source(), product.providerCode(), product.price(), null, today);
      PurchaseDecision decision = priceDecision(insight);
      result.add(new BriefingItem(
          product.productSq(),
          product.name(),
          product.price(),
          product.mallName(),
          product.source(),
          product.providerCode(),
          product.imageUrl(),
          product.productUrl(),
          product.imageSources(),
          "ORDER_RECOMMENDATION",
          null,
          null,
          null,
          decision.code(),
          decision.label(),
          priceNote(insight),
          insight
      ));
    }
    return List.copyOf(result);
  }

  private PriceInsight priceInsight(
      String provider,
      String providerCode,
      long currentPrice,
      LocalDate deadline,
      LocalDate today
  ) {
    Optional<ProductOfferEntity> offer = productOfferRepository
        .findByProviderAndExternalProductId(provider, providerCode);
    if (offer.isEmpty()) {
      return PriceInsight.collecting(currentPrice);
    }
    List<ProductPriceHistoryEntity> history = productPriceHistoryRepository
        .findAllByOfferSqAndCollectedAtGreaterThanEqualOrderByCollectedAtAsc(
            offer.get().getOfferSq(), LocalDateTime.now().minusDays(HISTORY_DAYS));
    if (history.isEmpty()) {
      return PriceInsight.collecting(currentPrice);
    }

    long lowest = currentPrice;
    long total = 0;
    for (ProductPriceHistoryEntity sample : history) {
      lowest = Math.min(lowest, sample.getTotalPrice());
      total += sample.getTotalPrice();
    }
    long average = Math.round((double) total / history.size());
    boolean historicalLow = history.size() >= 2 && currentPrice <= lowest;
    boolean nearHistoricalLow = history.size() >= 2 && currentPrice <= Math.round(lowest * 1.03d);
    TrendProjection projection = project(history, currentPrice, lowest, deadline, today);
    String status = history.size() >= MIN_FORECAST_SAMPLES ? "READY" : "COLLECTING";

    return new PriceInsight(
        HISTORY_DAYS,
        history.size(),
        currentPrice,
        lowest,
        average,
        historicalLow,
        nearHistoricalLow,
        projection.trend(),
        projection.expectedOptimalDate(),
        projection.basis(),
        projection.confidence(),
        status
    );
  }

  private TrendProjection project(
      List<ProductPriceHistoryEntity> history,
      long currentPrice,
      long lowestPrice,
      LocalDate deadline,
      LocalDate today
  ) {
    if (history.size() < MIN_FORECAST_SAMPLES) {
      return new TrendProjection("UNKNOWN", null, "가격 표본 수집 중", "LOW");
    }
    ProductPriceHistoryEntity first = history.getFirst();
    ProductPriceHistoryEntity last = history.getLast();
    double elapsedDays = Math.max(
        1.0d,
        Duration.between(first.getCollectedAt(), last.getCollectedAt()).toHours() / 24.0d
    );
    double dailyChange = (last.getTotalPrice() - first.getTotalPrice()) / elapsedDays;
    long stableThreshold = Math.max(100L, Math.round(currentPrice * 0.002d));
    if (dailyChange >= -stableThreshold) {
      String trend = dailyChange > stableThreshold ? "RISING" : "STABLE";
      return new TrendProjection(trend, null, "최근 30일 가격 추세", "LOW");
    }
    if (currentPrice <= lowestPrice) {
      return new TrendProjection("FALLING", today, "최근 30일 최저가 도달", confidence(history));
    }

    int expectedDays = (int) Math.ceil((currentPrice - lowestPrice) / -dailyChange);
    if (expectedDays < 1 || expectedDays > MAX_FORECAST_DAYS) {
      return new TrendProjection("FALLING", null, "하락 추세지만 예측 범위를 벗어남", "LOW");
    }
    LocalDate expectedDate = today.plusDays(expectedDays);
    if (deadline != null && expectedDate.isAfter(deadline)) {
      return new TrendProjection(
          "FALLING", null, "예상 최적일이 일정 구매 마감 이후", confidence(history));
    }
    return new TrendProjection(
        "FALLING",
        expectedDate,
        "최근 30일 단순 가격 추세 추정",
        confidence(history)
    );
  }

  private PurchaseDecision calendarDecision(LocalDate buyBy, PriceInsight insight, LocalDate today) {
    if (buyBy == null || !buyBy.isAfter(today.plusDays(2))) {
      return new PurchaseDecision("BUY_NOW", "지금 구매");
    }
    if (insight.historicalLow() || insight.nearHistoricalLow()) {
      return new PurchaseDecision("BUY_NOW", "가격 좋음");
    }
    if (insight.expectedOptimalDate() != null && !insight.expectedOptimalDate().isAfter(buyBy)) {
      return new PurchaseDecision("WAIT", DISPLAY_DATE.format(insight.expectedOptimalDate()) + " 확인");
    }
    return new PurchaseDecision("BUY_BY", DISPLAY_DATE.format(buyBy) + "까지 구매");
  }

  private PurchaseDecision priceDecision(PriceInsight insight) {
    if (insight.historicalLow()) {
      return new PurchaseDecision("BUY_NOW", "30일 최저가");
    }
    if (insight.nearHistoricalLow()) {
      return new PurchaseDecision("BUY_NOW", "최저가 근접");
    }
    if (insight.expectedOptimalDate() != null) {
      return new PurchaseDecision("WAIT", DISPLAY_DATE.format(insight.expectedOptimalDate()) + " 확인");
    }
    return new PurchaseDecision("PLAN", "가격 추적 중");
  }

  private String priceNote(PriceInsight insight) {
    if (insight.historicalLow()) {
      return "최근 30일 수집 가격 중 가장 낮은 가격이에요.";
    }
    if (insight.nearHistoricalLow()) {
      return "최근 30일 최저가에서 3% 이내인 가격이에요.";
    }
    if (insight.expectedOptimalDate() != null) {
      return DISPLAY_DATE.format(insight.expectedOptimalDate())
          + " 전후 최저가 접근 예상 · " + insight.forecastConfidence() + " 신뢰도";
    }
    return insight.sampleCount() < MIN_FORECAST_SAMPLES
        ? "구매 시점 예측을 위해 가격 표본을 수집 중이에요."
        : "뚜렷한 하락 추세가 없어 가격을 계속 확인하고 있어요.";
  }

  private String confidence(List<ProductPriceHistoryEntity> history) {
    Duration span = Duration.between(history.getFirst().getCollectedAt(), history.getLast().getCollectedAt());
    return history.size() >= 5 && span.toDays() >= 3 ? "MEDIUM" : "LOW";
  }

  private String productId(String provider, String providerCode) {
    return provider + "-" + providerCode;
  }

  private String providerName(String provider) {
    return "NAVER".equals(provider) ? "네이버쇼핑"
        : "ELEVENST".equals(provider) ? "11번가" : provider;
  }

  private List<BriefingItem> limit(List<BriefingItem> items, int size) {
    return List.copyOf(items.subList(0, Math.min(items.size(), size)));
  }

  private record PurchaseDecision(String code, String label) {
  }

  private record TrendProjection(
      String trend,
      LocalDate expectedOptimalDate,
      String basis,
      String confidence
  ) {
  }

  public record PriceInsight(
      int windowDays,
      int sampleCount,
      long currentPrice,
      long lowestPrice,
      long averagePrice,
      boolean historicalLow,
      boolean nearHistoricalLow,
      String trend,
      LocalDate expectedOptimalDate,
      String forecastBasis,
      String forecastConfidence,
      String status
  ) {
    private static PriceInsight collecting(long currentPrice) {
      return new PriceInsight(
          HISTORY_DAYS, 0, currentPrice, currentPrice, currentPrice,
          false, false, "UNKNOWN", null, "가격 표본 수집 중", "LOW", "COLLECTING");
    }
  }

  public record BriefingItem(
      String productSq,
      String name,
      long price,
      String mallName,
      String source,
      String providerCode,
      String imageUrl,
      String productUrl,
      ImageSources imageSources,
      String recommendationType,
      String eventTitle,
      String eventStartsAt,
      LocalDate recommendedBuyBy,
      String decision,
      String label,
      String note,
      PriceInsight priceInsight
  ) {
  }

  public record BriefingSection(
      String key,
      String title,
      String subtitle,
      List<BriefingItem> items
  ) {
  }

  public record BriefingView(
      String summary,
      List<BriefingSection> sections,
      boolean live,
      String dataMode,
      String recommendationBasis,
      List<String> interestKeywords,
      int priceWindowDays,
      LocalDateTime generatedAt
  ) {
  }

  public record AgentContext(
      String summary,
      String dataMode,
      List<BriefingItem> recommendations,
      BriefingView briefing,
      Map<String, Object> evidencePolicy
  ) {
    public static AgentContext empty() {
      return new AgentContext("", "NONE", List.of(), null, Map.of());
    }
  }
}
