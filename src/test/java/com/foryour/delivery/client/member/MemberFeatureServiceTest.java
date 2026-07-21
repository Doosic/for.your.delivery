package com.foryour.delivery.client.member;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberFeatureServiceTest {

  private final MemberFeatureService service = new MemberFeatureService();

  @Test
  void inventoryUpdateAndImportAreVisibleOnNextRead() {
    Long userSq = 10L;

    service.updateInventory(userSq, 1L, 72, 0.8, LocalDate.parse("2027-02-01"));
    List<Long> importedIds = service.importInventoryItems(userSq, List.of(3L));

    MemberFeatureService.InventoryView inventory = service.inventory(userSq, "ALL");
    assertThat(inventory.items())
        .filteredOn(item -> item.inventoryItemSq().equals(1L))
        .singleElement()
        .satisfies(item -> {
          assertThat(item.remainingRatio()).isEqualTo(72);
          assertThat(item.quantity()).isEqualTo(0.8);
          assertThat(item.expiryDate()).isEqualTo("2027-02-01");
        });
    assertThat(importedIds).containsExactly(1_003L);
    assertThat(inventory.items()).extracting(MemberFeatureService.InventoryItem::inventoryItemSq)
        .contains(1_003L);
  }

  @Test
  void wikiUpdateIncrementsVersionAndKeepsUsersSeparated() {
    MemberFeatureService.WikiEntry updated = service.updateWiki(20L, 1L, "SHOPPING", "당일 배송 선호");

    assertThat(updated.version()).isEqualTo(2);
    assertThat(service.wiki(20L, "사용자").entries()).contains(updated);
    assertThat(service.wiki(21L, "다른 사용자").entries()).noneMatch(entry -> entry.content().equals("당일 배송 선호"));
  }

  @Test
  void recommendationAlertAppearsInNotificationList() {
    MemberFeatureService.NotificationItem scheduled = service.scheduleRecommendationAlert(30L, 77L);

    MemberFeatureService.NotificationPage page = service.notificationPage(30L, 0, 20);
    assertThat(page.notifications().getFirst()).isEqualTo(scheduled);
    assertThat(scheduled.refId()).isEqualTo(77L);
  }

  @Test
  void reportAndBundleFollowSpecificationShape() {
    MemberFeatureService.MonthlyReport report = service.monthlyReport(40L, YearMonth.parse("2026-07"));
    MemberFeatureService.BundleResult bundle = service.optimizeBundles(40L, List.of(1L, 2L));

    assertThat(report.month()).isEqualTo("2026-07");
    assertThat(report.categories()).isNotEmpty();
    assertThat(bundle.totalSaving()).isPositive();
    assertThat(bundle.bundles()).singleElement().satisfies(item ->
        assertThat(item.recommendationSqs()).containsExactly(1L, 2L));
  }
}
