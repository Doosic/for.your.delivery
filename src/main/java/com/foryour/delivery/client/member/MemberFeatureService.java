package com.foryour.delivery.client.member;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MemberFeatureService {

  private final Map<Long, Map<Long, InventoryItem>> inventories = new ConcurrentHashMap<>();
  private final Map<Long, Map<Long, WikiEntry>> wikiEntries = new ConcurrentHashMap<>();
  private final Map<Long, List<NotificationItem>> notifications = new ConcurrentHashMap<>();
  private final AtomicLong notificationSequence = new AtomicLong(100);

  public BriefingRefresh refreshBriefing(Long userSq) {
    return new BriefingRefresh(
        "brief-" + UUID.randomUUID(),
        "trace-" + userSq + "-" + Instant.now().toEpochMilli(),
        "QUEUED",
        Instant.now().toString()
    );
  }

  public RecommendationDetail recommendation(Long userSq, Long recommendationSq) {
    String decision = recommendationSq % 2 == 0 ? "WAIT" : "BUY_NOW";
    return new RecommendationDetail(
        recommendationSq,
        decision,
        decision.equals("BUY_NOW") ? 0.87 : 0.68,
        List.of(
            new Reason("INVENTORY", "예상 소진일까지 3일 남았습니다.", 0.92),
            new Reason("PRICE", "최근 30일 평균보다 8% 낮은 가격입니다.", 0.84),
            new Reason("DELIVERY", "권장일 전에 도착 가능한 상품입니다.", 0.90)
        ),
        List.of(
            new PriceComparison("NAVER", "네이버쇼핑", 18_900, 0, "2026-07-23"),
            new PriceComparison("ELEVENST", "11번가", 19_400, 0, "2026-07-24")
        ),
        "decision-demo-v1",
        false
    );
  }

  public NotificationItem scheduleRecommendationAlert(Long userSq, Long recommendationSq) {
    NotificationItem notification = new NotificationItem(
        notificationSequence.incrementAndGet(),
        "RECOMMENDATION_DUE",
        recommendationSq,
        "구매 권장일 알림",
        "추천 상품의 구매 권장일이 다가왔어요.",
        Instant.now().plusSeconds(86_400).toString(),
        null,
        null,
        false
    );
    userNotifications(userSq).add(0, notification);
    return notification;
  }

  public ChatSession chatSession(Long userSq, Long sessionSq) {
    return new ChatSession(
        sessionSq,
        List.of(
            new ChatMessage(1L, "user", "다음 주 캠핑 준비물을 찾아줘", null, "2026-07-21T09:00:00Z"),
            new ChatMessage(2L, "assistant", "재고와 배송일을 기준으로 우선 준비할 품목을 정리했어요.",
                Map.of("title", "캠핑 준비", "items", List.of("모기퇴치제", "아이스박스", "숯")),
                "2026-07-21T09:00:02Z")
        ),
        "chat-session-demo-v1",
        false
    );
  }

  public InventoryView inventory(Long userSq, String status) {
    List<InventoryItem> items = new ArrayList<>(userInventory(userSq).values());
    items.sort(new Comparator<InventoryItem>() {
      @Override
      public int compare(InventoryItem first, InventoryItem second) {
        return first.inventoryItemSq().compareTo(second.inventoryItemSq());
      }
    });
    if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
      List<InventoryItem> filteredItems = new ArrayList<>();
      for (InventoryItem item : items) {
        if (item.status().equalsIgnoreCase(status)) {
          filteredItems.add(item);
        }
      }
      items = filteredItems;
    }
    return new InventoryView(items, items.size(), false);
  }

  public InventoryItem updateInventory(
      Long userSq,
      Long inventoryItemSq,
      Integer remainingRatio,
      Double quantity,
      LocalDate expiryDate
  ) {
    Map<Long, InventoryItem> inventory = userInventory(userSq);
    InventoryItem current = inventory.getOrDefault(inventoryItemSq, defaultInventoryItem(inventoryItemSq));
    InventoryItem updated = new InventoryItem(
        current.inventoryItemSq(),
        current.name(),
        current.category(),
        quantity == null ? current.quantity() : Math.max(0, quantity),
        current.unit(),
        remainingRatio == null ? current.remainingRatio() : Math.max(0, Math.min(100, remainingRatio)),
        expiryDate == null ? current.expiryDate() : expiryDate.toString(),
        current.estimatedDepletionAt(),
        current.status(),
        "USER",
        1.0,
        Instant.now().toString()
    );
    inventory.put(inventoryItemSq, updated);
    return updated;
  }

  public List<Long> importInventoryItems(Long userSq, List<Long> importedItemSqs) {
    Map<Long, InventoryItem> inventory = userInventory(userSq);
    List<Long> inventoryItemSqs = new ArrayList<>();
    for (Long importedItemSq : importedItemSqs) {
      long inventoryItemSq = 1_000 + importedItemSq;
      String name;
      switch (importedItemSq.intValue()) {
        case 1:
          name = "고양이 사료 오리진 1.5kg";
          break;
        case 2:
          name = "세탁세제 리필 2.6L";
          break;
        case 3:
          name = "우유 900ml x2";
          break;
        case 4:
          name = "물티슈 캡형 10팩";
          break;
        default:
          name = "가져온 구매 품목 " + importedItemSq;
          break;
      }
      inventory.putIfAbsent(inventoryItemSq, new InventoryItem(
          inventoryItemSq,
          name,
          "IMPORTED",
          1,
          "each",
          100,
          null,
          null,
          "ACTIVE",
          "IMPORT",
          0.85,
          Instant.now().toString()
      ));
      inventoryItemSqs.add(inventoryItemSq);
    }
    return inventoryItemSqs;
  }

  public WikiView wiki(Long userSq, String userName) {
    List<WikiEntry> entries = new ArrayList<>(userWiki(userSq).values());
    entries.sort(new Comparator<WikiEntry>() {
      @Override
      public int compare(WikiEntry first, WikiEntry second) {
        return first.wikiEntrySq().compareTo(second.wikiEntrySq());
      }
    });
    return new WikiView(
        Map.of(
            "name", userName,
            "householdSize", 1,
            "pets", List.of("고양이"),
            "preferredChannels", List.of("NAVER", "ELEVENST")
        ),
        entries,
        List.of(
            Map.of("pattern", "고양이 사료는 약 28일 주기로 구매", "confidence", 0.82),
            Map.of("pattern", "무료배송 상품 선호", "confidence", 0.76)
        ),
        false
    );
  }

  public WikiEntry updateWiki(Long userSq, Long wikiEntrySq, String category, String content) {
    Map<Long, WikiEntry> wiki = userWiki(userSq);
    WikiEntry current = wiki.getOrDefault(wikiEntrySq, defaultWikiEntry(wikiEntrySq));
    WikiEntry updated = new WikiEntry(
        wikiEntrySq,
        category,
        content,
        "EXPLICIT",
        "USER",
        1.0,
        null,
        "ACTIVE",
        current.version() + 1,
        Instant.now().toString()
    );
    wiki.put(wikiEntrySq, updated);
    return updated;
  }

  public MonthlyReport monthlyReport(Long userSq, YearMonth month) {
    return new MonthlyReport(
        month.toString(),
        187_600,
        24_300,
        11,
        List.of(
            new CategorySpend("반려동물", 83_200, 44),
            new CategorySpend("생활용품", 61_500, 33),
            new CategorySpend("식품", 42_900, 23)
        ),
        List.of(
            "정기 구매 시점을 맞춰 긴급 배송비를 약 8,000원 줄였어요.",
            "고양이 모래 구매량이 지난달보다 12% 늘었습니다."
        ),
        false
    );
  }

  public BundleResult optimizeBundles(Long userSq, List<Long> recommendationSqs) {
    List<Long> ids = recommendationSqs == null ? List.of() : List.copyOf(recommendationSqs);
    long baseSaving = ids.size() * 1_700L;
    return new BundleResult(
        baseSaving,
        List.of(new Bundle(
            "NAVER",
            "네이버쇼핑 묶음 배송",
            ids,
            ids.size() * 18_900L,
            baseSaving,
            0,
            "2026-07-24"
        )),
        "bundle-demo-v1",
        false
    );
  }

  public NotificationPage notificationPage(Long userSq, int page, int size) {
    List<NotificationItem> all = userNotifications(userSq);
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(size, 100));
    int from = Math.min(safePage * safeSize, all.size());
    int to = Math.min(from + safeSize, all.size());
    return new NotificationPage(List.copyOf(all.subList(from, to)), safePage, safeSize, all.size(), false);
  }

  private Map<Long, InventoryItem> userInventory(Long userSq) {
    Map<Long, InventoryItem> values = inventories.get(userSq);
    if (values == null) {
      values = new ConcurrentHashMap<>();
      values.put(1L, new InventoryItem(1L, "고양이 사료 1.5kg", "PET_FOOD", 0.35, "bag", 35,
          "2027-01-31", "2026-07-25", "LOW", "PURCHASE", 0.86, Instant.now().toString()));
      values.put(2L, new InventoryItem(2L, "고양이 모래 6L", "PET_SUPPLY", 1.0, "pack", 50,
          null, "2026-07-28", "ACTIVE", "PURCHASE", 0.79, Instant.now().toString()));
      values.put(3L, new InventoryItem(3L, "세탁세제 리필 2.6L", "HOUSEHOLD", 0.2, "pack", 20,
          "2028-03-01", "2026-07-23", "LOW", "IMPORT", 0.91, Instant.now().toString()));
      inventories.put(userSq, values);
    }
    return values;
  }

  private InventoryItem defaultInventoryItem(Long inventoryItemSq) {
    return new InventoryItem(inventoryItemSq, "사용자 입력 품목", "ETC", 0, "each", 0,
        null, null, "ACTIVE", "USER", 1.0, Instant.now().toString());
  }

  private Map<Long, WikiEntry> userWiki(Long userSq) {
    Map<Long, WikiEntry> values = wikiEntries.get(userSq);
    if (values == null) {
      values = new ConcurrentHashMap<>();
      values.put(1L, new WikiEntry(1L, "PET", "고양이 한 마리와 함께 생활", "EXPLICIT", "ONBOARDING",
          1.0, null, "ACTIVE", 1, Instant.now().toString()));
      values.put(2L, new WikiEntry(2L, "SHOPPING", "무료배송과 도착 예정일을 중요하게 판단", "INFERRED", "FEEDBACK",
          0.78, "2026-10-21", "ACTIVE", 1, Instant.now().toString()));
      wikiEntries.put(userSq, values);
    }
    return values;
  }

  private WikiEntry defaultWikiEntry(Long wikiEntrySq) {
    return new WikiEntry(wikiEntrySq, "ETC", "", "EXPLICIT", "USER", 1.0,
        null, "ACTIVE", 0, Instant.now().toString());
  }

  private List<NotificationItem> userNotifications(Long userSq) {
    List<NotificationItem> values = notifications.get(userSq);
    if (values == null) {
      values = new CopyOnWriteArrayList<>(List.of(
          new NotificationItem(1L, "PRICE_DROP", 1L, "가격이 내려갔어요", "고양이 사료가 최근 평균보다 8% 저렴해요.",
              "2026-07-21T10:00:00Z", "2026-07-21T10:00:03Z", null, false),
          new NotificationItem(2L, "PURCHASE_DUE", 2L, "구매 시점이 다가왔어요", "고양이 모래 재고를 확인해 주세요.",
              "2026-07-22T09:00:00Z", null, null, false)
      ));
      notifications.put(userSq, values);
    }
    return values;
  }

  public record BriefingRefresh(String jobId, String traceId, String status, String requestedAt) {
  }

  public record RecommendationDetail(
      Long recommendationSq,
      String decision,
      double score,
      List<Reason> reasons,
      List<PriceComparison> priceComparison,
      String modelVersion,
      boolean live
  ) {
  }

  public record Reason(String code, String description, double score) {
  }

  public record PriceComparison(String source, String seller, long price, long shippingFee, String eta) {
  }

  public record ChatSession(Long sessionSq, List<ChatMessage> messages, String traceId, boolean live) {
  }

  public record ChatMessage(Long chatMessageSq, String role, String content, Map<String, Object> card, String createdAt) {
  }

  public record InventoryView(List<InventoryItem> items, int totalCount, boolean live) {
  }

  public record InventoryItem(
      Long inventoryItemSq,
      String name,
      String category,
      double quantity,
      String unit,
      int remainingRatio,
      String expiryDate,
      String estimatedDepletionAt,
      String status,
      String source,
      double confidence,
      String updatedAt
  ) {
  }

  public record WikiView(
      Map<String, Object> profile,
      List<WikiEntry> entries,
      List<Map<String, Object>> learnedPatterns,
      boolean live
  ) {
  }

  public record WikiEntry(
      Long wikiEntrySq,
      String category,
      String content,
      String explicitOrInferred,
      String source,
      double confidence,
      String expiresAt,
      String status,
      int version,
      String updatedAt
  ) {
  }

  public record MonthlyReport(
      String month,
      long totalSpend,
      long savedAmount,
      int purchaseCount,
      List<CategorySpend> categories,
      List<String> insights,
      boolean live
  ) {
  }

  public record CategorySpend(String category, long amount, int percentage) {
  }

  public record BundleResult(long totalSaving, List<Bundle> bundles, String modelVersion, boolean live) {
  }

  public record Bundle(
      String source,
      String title,
      List<Long> recommendationSqs,
      long productTotal,
      long saving,
      long shippingFee,
      String eta
  ) {
  }

  public record NotificationPage(
      List<NotificationItem> notifications,
      int page,
      int size,
      int totalCount,
      boolean live
  ) {
  }

  public record NotificationItem(
      Long notificationSq,
      String type,
      Long refId,
      String title,
      String body,
      String scheduledAt,
      String sentAt,
      String suppressedReason,
      boolean read
  ) {
  }
}
