package com.foryour.delivery.client.agent;

import com.foryour.delivery.client.calendar.GoogleCalendarService;
import com.foryour.delivery.client.product.ProductModels.HomeFeedResponse;
import com.foryour.delivery.client.product.ProductModels.ImageSources;
import com.foryour.delivery.client.product.ProductModels.ProductFeedItem;
import com.foryour.delivery.client.product.ProductService;
import com.foryour.delivery.domain.entity.ProductOfferEntity;
import com.foryour.delivery.domain.entity.ProductPriceHistoryEntity;
import com.foryour.delivery.domain.enums.AgentTypeCode;
import com.foryour.delivery.domain.repository.ProductOfferRepository;
import com.foryour.delivery.domain.repository.ProductPriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AgentBriefingHarnessTest {

  @Mock
  private ProductService productService;

  @Mock
  private GoogleCalendarService googleCalendarService;

  @Mock
  private ProductOfferRepository productOfferRepository;

  @Mock
  private ProductPriceHistoryRepository productPriceHistoryRepository;

  private AgentBriefingHarness harness;

  @BeforeEach
  void setUp() {
    harness = new AgentBriefingHarness(
        productService,
        googleCalendarService,
        productOfferRepository,
        productPriceHistoryRepository
    );
  }

  @Test
  void combinesCalendarDeadlineAndPriceTrendIntoPurchaseTiming() {
    LocalDate today = LocalDate.now();
    ProductFeedItem priceProduct = new ProductFeedItem(
        "NAVER-PRICE-1", "캠핑 랜턴", 13_000, "네이버쇼핑", "NAVER", "image", "url",
        "PRICE-1", new ImageSources("card", "card2x", "detail", "original"),
        1, 10_000, false, "READY", "NAVER_LPRICE", 4);
    given(productService.homeFeed(7L)).willReturn(new HomeFeedResponse(
        true, List.of(), true, false, "PERSONAL_WIKI", List.of("캠핑 용품"),
        List.of(), List.of(priceProduct)));

    GoogleCalendarService.SuggestedProduct calendarProduct =
        new GoogleCalendarService.SuggestedProduct(
            "캠핑 모기퇴치제", "NAVER", "CAL-1", "calendar-url", "calendar-image", 9_900L);
    GoogleCalendarService.PreparationItem preparation =
        new GoogleCalendarService.PreparationItem(
            1L, "모기퇴치제", "야외 일정 방충 준비", today.plusDays(1), "HIGH", true,
            calendarProduct);
    GoogleCalendarService.CalendarSuggestion event =
        new GoogleCalendarService.CalendarSuggestion(
            1L, "event-1", "주말 캠핑", today.plusDays(3) + " 09:00", "가평",
            "모기퇴치제를 준비하세요.", List.of(preparation));
    given(googleCalendarService.storedSuggestions(eq(7L), eq(today), eq(today.plusDays(30))))
        .willReturn(new GoogleCalendarService.CalendarResult(false, true, List.of(event), null));

    ProductOfferEntity calendarOffer = offer(11L, "CAL-1");
    ProductOfferEntity priceOffer = offer(12L, "PRICE-1");
    given(productOfferRepository.findByProviderAndExternalProductId("NAVER", "CAL-1"))
        .willReturn(Optional.of(calendarOffer));
    given(productOfferRepository.findByProviderAndExternalProductId("NAVER", "PRICE-1"))
        .willReturn(Optional.of(priceOffer));
    given(productPriceHistoryRepository
        .findAllByOfferSqAndCollectedAtGreaterThanEqualOrderByCollectedAtAsc(eq(11L), any()))
        .willReturn(List.of(
            price(11L, 11_000, LocalDateTime.now().minusDays(3)),
            price(11L, 10_400, LocalDateTime.now().minusDays(1)),
            price(11L, 9_900, LocalDateTime.now())
        ));
    given(productPriceHistoryRepository
        .findAllByOfferSqAndCollectedAtGreaterThanEqualOrderByCollectedAtAsc(eq(12L), any()))
        .willReturn(List.of(
            price(12L, 16_000, LocalDateTime.now().minusDays(4)),
            price(12L, 10_000, LocalDateTime.now().minusDays(3)),
            price(12L, 15_000, LocalDateTime.now().minusDays(2)),
            price(12L, 13_000, LocalDateTime.now())
        ));

    AgentBriefingHarness.BriefingView result = harness.today(7L);

    AgentBriefingHarness.BriefingItem calendarItem = result.sections().getFirst().items().getFirst();
    AgentBriefingHarness.BriefingItem timingItem = result.sections().get(2).items().getFirst();
    assertThat(calendarItem.decision()).isEqualTo("BUY_NOW");
    assertThat(calendarItem.recommendedBuyBy()).isEqualTo(today.plusDays(1));
    assertThat(calendarItem.priceInsight().historicalLow()).isTrue();
    assertThat(timingItem.decision()).isEqualTo("WAIT");
    assertThat(timingItem.priceInsight().expectedOptimalDate()).isAfter(today);
    assertThat(timingItem.priceInsight().forecastBasis()).contains("가격 추세");

    AgentBriefingHarness.AgentContext focused =
        harness.contextFor(7L, AgentTypeCode.BRIEFING_SHOPPING, "캠핑 랜턴 가격 알려줘");
    assertThat(focused.recommendations())
        .extracting(AgentBriefingHarness.BriefingItem::name)
        .containsExactly("캠핑 랜턴");
  }

  private ProductOfferEntity offer(Long offerSq, String providerCode) {
    ProductOfferEntity offer = new ProductOfferEntity();
    offer.setOfferSq(offerSq);
    offer.setProvider("NAVER");
    offer.setExternalProductId(providerCode);
    return offer;
  }

  private ProductPriceHistoryEntity price(Long offerSq, long totalPrice, LocalDateTime collectedAt) {
    ProductPriceHistoryEntity history = new ProductPriceHistoryEntity();
    history.setOfferSq(offerSq);
    history.setPrice(totalPrice);
    history.setTotalPrice(totalPrice);
    history.setCollectedAt(collectedAt);
    return history;
  }
}
