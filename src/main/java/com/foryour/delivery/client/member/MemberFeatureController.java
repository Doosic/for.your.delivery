package com.foryour.delivery.client.member;

import com.foryour.delivery.client.member.MemberFeatureService.BriefingRefresh;
import com.foryour.delivery.client.member.MemberFeatureService.BundleResult;
import com.foryour.delivery.client.member.MemberFeatureService.ChatSession;
import com.foryour.delivery.client.member.MemberFeatureService.InventoryItem;
import com.foryour.delivery.client.member.MemberFeatureService.InventoryView;
import com.foryour.delivery.client.member.MemberFeatureService.MonthlyReport;
import com.foryour.delivery.client.member.MemberFeatureService.NotificationItem;
import com.foryour.delivery.client.member.MemberFeatureService.NotificationPage;
import com.foryour.delivery.client.member.MemberFeatureService.RecommendationDetail;
import com.foryour.delivery.client.member.MemberFeatureService.WikiEntry;
import com.foryour.delivery.client.member.MemberFeatureService.WikiView;
import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberFeatureController extends BaseController {

  private final MemberFeatureService memberFeatureService;

  @PostMapping("/wb/briefing/refresh")
  public APIDataResponse<BriefingRefresh> refreshBriefing() {
    return APIDataResponse.of(memberFeatureService.refreshBriefing(getSessionInfo().getUserSq()));
  }

  @GetMapping("/wb/recommendation/{recommendationSq}")
  public APIDataResponse<RecommendationDetail> recommendation(@PathVariable Long recommendationSq) {
    return APIDataResponse.of(memberFeatureService.recommendation(getSessionInfo().getUserSq(), recommendationSq));
  }

  @PostMapping("/wb/recommendation/{recommendationSq}/alert")
  public APIDataResponse<NotificationItem> recommendationAlert(@PathVariable Long recommendationSq) {
    return APIDataResponse.of(memberFeatureService.scheduleRecommendationAlert(
        getSessionInfo().getUserSq(), recommendationSq));
  }

  @GetMapping("/wb/chat/session/{sessionSq}")
  public APIDataResponse<ChatSession> chatSession(@PathVariable Long sessionSq) {
    return APIDataResponse.of(memberFeatureService.chatSession(getSessionInfo().getUserSq(), sessionSq));
  }

  @GetMapping("/wb/inventory")
  public APIDataResponse<InventoryView> inventory(@RequestParam(defaultValue = "ALL") String status) {
    return APIDataResponse.of(memberFeatureService.inventory(getSessionInfo().getUserSq(), status));
  }

  @PostMapping("/wb/inventory/{inventoryItemSq}/update")
  public APIDataResponse<InventoryItem> updateInventory(
      @PathVariable Long inventoryItemSq,
      @Valid @RequestBody InventoryUpdateRequest request
  ) {
    return APIDataResponse.of(memberFeatureService.updateInventory(
        getSessionInfo().getUserSq(),
        inventoryItemSq,
        request.remainingRatio(),
        request.quantity(),
        request.expiryDate() == null || request.expiryDate().isBlank() ? null : LocalDate.parse(request.expiryDate())
    ));
  }

  @GetMapping("/wb/wiki")
  public APIDataResponse<WikiView> wiki() {
    UserResponseVO user = getSessionInfo();
    return APIDataResponse.of(memberFeatureService.wiki(user.getUserSq(), user.getName()));
  }

  @PostMapping("/wb/wiki/entry/{wikiEntrySq}/update")
  public APIDataResponse<WikiEntry> updateWiki(
      @PathVariable Long wikiEntrySq,
      @Valid @RequestBody WikiUpdateRequest request
  ) {
    return APIDataResponse.of(memberFeatureService.updateWiki(
        getSessionInfo().getUserSq(), wikiEntrySq, request.category(), request.content()));
  }

  @GetMapping("/wb/report/monthly")
  public APIDataResponse<MonthlyReport> monthlyReport(
      @RequestParam(defaultValue = "2026-07") String month
  ) {
    return APIDataResponse.of(memberFeatureService.monthlyReport(
        getSessionInfo().getUserSq(), YearMonth.parse(month)));
  }

  @PostMapping("/wb/bundle/optimize")
  public APIDataResponse<BundleResult> optimizeBundle(@Valid @RequestBody BundleRequest request) {
    return APIDataResponse.of(memberFeatureService.optimizeBundles(
        getSessionInfo().getUserSq(), request.recommendationSqs()));
  }

  @GetMapping("/wb/notification/list")
  public APIDataResponse<NotificationPage> notifications(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return APIDataResponse.of(memberFeatureService.notificationPage(getSessionInfo().getUserSq(), page, size));
  }

  public record InventoryUpdateRequest(
      @Min(0) @Max(100) Integer remainingRatio,
      @PositiveOrZero Double quantity,
      String expiryDate
  ) {
  }

  public record WikiUpdateRequest(@NotBlank String category, @NotBlank String content) {
  }

  public record BundleRequest(@NotEmpty List<@Positive Long> recommendationSqs) {
  }
}
