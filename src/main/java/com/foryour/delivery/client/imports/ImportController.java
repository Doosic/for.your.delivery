package com.foryour.delivery.client.imports;

import com.foryour.delivery.client.calendar.GoogleCalendarService;
import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
public class ImportController extends BaseController {

  private final GoogleCalendarService googleCalendarService;
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
            googleConnected, googleConnected ? "필요할 때 동기화" : null, googleConnected ? null : "Google 계정 연결"),
        connection(2, "GMAIL", "Gmail 주문메일", "Gmail 권한 키 연결 후 주문확인 메일 수집", false, null, "연결 준비 중"),
        connection(3, "CODEF", "구매내역 API", "CODEF Connected ID 연동을 위한 서버 설정 대기", false, null, "API 키 필요"),
        connection(4, "FILE", "파일 업로드", "주문내역 CSV 또는 엑셀 업로드", false, null, "파일 선택")
        )));
  }

  @PostMapping("/wp/import-connections/oauth/start")
  public APIDataResponse<Map<String, Object>> oauthStart(@RequestBody OAuthStartRequest request) {
    getSessionInfo();
    if ("GOOGLE_CALENDAR".equalsIgnoreCase(request.source())) {
      return APIDataResponse.of(Map.of(
          "source", "GOOGLE_CALENDAR",
          "authorizationUrl", "/delivery/oauth2/authorization/google",
          "connected", false
      ));
    }
    return APIDataResponse.of(Map.of(
        "source", request.source(),
        "connected", false,
        "requiresConfiguration", true
    ));
  }

  @PostMapping("/wp/imports/sync")
  public APIDataResponse<Map<String, Object>> sync(@RequestBody SyncRequest request) {
    getSessionInfo();
    return APIDataResponse.of(Map.of(
        "importedCount", demoItems().size(),
        "newCount", demoItems().size(),
        "sources", request.sources() == null ? List.of() : request.sources(),
        "live", false
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
    List<ImportedItem> items = demoItems().stream()
        .filter(item -> !committed.contains(item.importedItemSq()))
        .limit(Math.max(1, Math.min(size, 100)))
        .toList();
    return APIDataResponse.of(Map.of(
        "items", items,
        "page", Math.max(page, 0),
        "status", status,
        "live", false
    ));
  }

  @PostMapping("/wp/imports/items/commit")
  public APIDataResponse<Map<String, Object>> commit(@RequestBody CommitRequest request) {
    Long userSq = getSessionInfo().getUserSq();
    List<Long> itemSqs = request.importedItemSqs() == null ? List.of() : request.importedItemSqs();
    committedItems.computeIfAbsent(userSq, ignored -> ConcurrentHashMap.newKeySet()).addAll(itemSqs);
    return APIDataResponse.of(Map.of("committedCount", itemSqs.size(), "live", false));
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

  private List<ImportedItem> demoItems() {
    return List.of(
        new ImportedItem(1L, "고양이 사료 오리진 1.5kg", 1, 32400, "2026-07-21", "GMAIL", true),
        new ImportedItem(2L, "세탁세제 리필 2.6L", 1, 12900, "2026-07-20", "FILE", true),
        new ImportedItem(3L, "우유 900ml x2", 2, 5600, "2026-07-19", "GMAIL", true),
        new ImportedItem(4L, "물티슈 캡형 10팩", 1, 9900, "2026-07-18", "FILE", false)
    );
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
      boolean isNew
  ) {
  }
}
