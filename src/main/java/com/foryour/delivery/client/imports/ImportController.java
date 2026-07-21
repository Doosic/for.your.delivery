package com.foryour.delivery.client.imports;

import com.foryour.delivery.client.calendar.GoogleCalendarAuthorizationService;
import com.foryour.delivery.client.calendar.GoogleCalendarService;
import com.foryour.delivery.client.member.MemberFeatureService;
import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import com.foryour.delivery.domain.entity.ProductEntity;
import com.foryour.delivery.domain.entity.PurchaseClickEntity;
import com.foryour.delivery.domain.repository.ProductRepository;
import com.foryour.delivery.domain.repository.PurchaseClickRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
public class ImportController extends BaseController {

  private final GoogleCalendarService googleCalendarService;
  private final GoogleCalendarAuthorizationService googleCalendarAuthorizationService;
  private final MemberFeatureService memberFeatureService;
  private final PurchaseClickRepository purchaseClickRepository;
  private final ProductRepository productRepository;
  private final Map<Long, Set<Long>> committedItems = new ConcurrentHashMap<>();

  @GetMapping("/wp/import-connections")
  public APIDataResponse<Map<String, Object>> connections() {
    UserResponseVO user = getSessionInfo();
    boolean googleConnected = googleCalendarService.isConnected(user.getEmail());
    return APIDataResponse.of(Map.of(
        "live", googleConnected,
        "connections", List.of(
        connection(1, "GOOGLE_CALENDAR", "Google Calendar",
            googleConnected ? "연결됨 · 일정 읽기 권한 사용 중" : "일정에서 준비물과 구매 마감일 추출",
            googleConnected, googleConnected ? "필요할 때 동기화" : null, googleConnected ? null : "Calendar 권한 연결"),
        connection(2, "GMAIL", "Gmail 주문메일", "Gmail 권한 키 연결 후 주문확인 메일 수집", false, null, "연결 준비 중")
        )));
  }

  @PostMapping("/wp/import-connections/oauth/start")
  public APIDataResponse<Map<String, Object>> oauthStart(
      @RequestBody OAuthStartRequest request,
      HttpServletRequest servletRequest
  ) {
    UserResponseVO user = getSessionInfo();
    if ("GOOGLE_CALENDAR".equalsIgnoreCase(request.source())) {
      return APIDataResponse.of(Map.of(
          "source", "GOOGLE_CALENDAR",
          "authorizationUrl", googleCalendarAuthorizationService.authorizationUrl(
              user.getEmail(), servletRequest),
          "connected", false
      ));
    }
    return APIDataResponse.of(Map.of(
        "source", request.source(),
        "connected", false,
        "requiresConfiguration", true
    ));
  }

  @GetMapping("/wp/calendar/google/callback")
  public void googleCalendarCallback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    response.sendRedirect(googleCalendarAuthorizationService.complete(code, state, error, request));
  }

  @PostMapping("/wp/imports/sync")
  public APIDataResponse<Map<String, Object>> sync(@RequestBody SyncRequest request) {
    Long userSq = getSessionInfo().getUserSq();
    int purchaseCount = purchaseClickRepository.findAllByUserSqOrderByClickedAtDesc(userSq).size();
    return APIDataResponse.of(Map.of(
        "importedCount", purchaseCount,
        "newCount", purchaseCount,
        "sources", request.sources() == null ? List.of() : request.sources(),
        "live", true
    ));
  }

  @GetMapping("/wp/imports/items")
  public APIDataResponse<Map<String, Object>> items(
      @RequestParam(defaultValue = "NEW") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Long userSq = getSessionInfo().getUserSq();
    Set<Long> committed = committedItems.getOrDefault(userSq, Set.of());
    List<PurchaseClickEntity> clicks = purchaseClickRepository.findAllByUserSqOrderByClickedAtDesc(userSq);
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(1, Math.min(size, 100));
    int fromIndex = Math.min(safePage * safeSize, clicks.size());
    int toIndex = Math.min(fromIndex + safeSize, clicks.size());
    List<ImportedItem> items = new ArrayList<>();
    for (PurchaseClickEntity click : clicks.subList(fromIndex, toIndex)) {
      if (committed.contains(click.getPurchaseClickSq())) {
        continue;
      }
      ProductEntity product = productRepository.findById(click.getProductSq()).orElse(null);
      items.add(new ImportedItem(
          click.getPurchaseClickSq(),
          product == null ? "구매 상품" : product.getName(),
          1,
          click.getPriceAtClick(),
          click.getClickedAt().format(DateTimeFormatter.ISO_LOCAL_DATE),
          click.getProvider(),
          true,
          product == null ? null : product.getImageUrl(),
          click.getTargetUrl()
      ));
    }
    return APIDataResponse.of(Map.of(
        "items", items,
        "page", safePage,
        "status", status,
        "live", true
    ));
  }

  @PostMapping("/wp/imports/items/commit")
  public APIDataResponse<Map<String, Object>> commit(@RequestBody CommitRequest request) {
    Long userSq = getSessionInfo().getUserSq();
    List<Long> itemSqs = request.importedItemSqs() == null ? List.of() : request.importedItemSqs();
    committedItems.computeIfAbsent(userSq, ignored -> ConcurrentHashMap.newKeySet()).addAll(itemSqs);
    List<Long> inventoryItemSqs = memberFeatureService.importInventoryItems(userSq, itemSqs);
    return APIDataResponse.of(Map.of(
        "committedCount", itemSqs.size(),
        "inventoryItemSqs", inventoryItemSqs,
        "live", true
    ));
  }

  private Map<String, Object> connection(
      long sq,
      String source,
      String label,
      String description,
      boolean connected,
      String autoSync,
      String action
  ) {
    Map<String, Object> value = new ConcurrentHashMap<>();
    value.put("importConnectionSq", sq);
    value.put("source", source);
    value.put("label", label);
    value.put("description", description);
    value.put("connected", connected);
    if (autoSync != null) value.put("autoSync", autoSync);
    if (action != null) value.put("action", action);
    return value;
  }

  public record OAuthStartRequest(String source, String redirectUri) {
  }

  public record SyncRequest(List<String> sources, String from, String to) {
  }

  public record CommitRequest(List<Long> importedItemSqs) {
  }

  public record ImportedItem(
      Long importedItemSq,
      String name,
      int quantity,
      long price,
      String purchasedAt,
      String source,
      boolean isNew,
      String imageUrl,
      String productUrl
  ) {
  }
}
