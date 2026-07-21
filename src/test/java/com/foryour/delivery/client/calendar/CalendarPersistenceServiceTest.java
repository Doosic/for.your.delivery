package com.foryour.delivery.client.calendar;

import com.foryour.delivery.client.calendar.CalendarPersistenceService.CalendarEventInput;
import com.foryour.delivery.client.calendar.CalendarPersistenceService.CalendarSuggestionInput;
import com.foryour.delivery.client.calendar.CalendarPersistenceService.ProductSnapshot;
import com.foryour.delivery.client.product.ProductCatalogService;
import com.foryour.delivery.client.product.ProductModels.ImageSources;
import com.foryour.delivery.client.product.ProductModels.ProductItem;
import com.foryour.delivery.domain.entity.CalendarEventEntity;
import com.foryour.delivery.domain.entity.UserEntity;
import com.foryour.delivery.domain.enums.UserStatusCode;
import com.foryour.delivery.domain.repository.CalendarEventRepository;
import com.foryour.delivery.domain.repository.CalendarItemSuggestionRepository;
import com.foryour.delivery.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CalendarPersistenceServiceTest {

  @Autowired
  private CalendarPersistenceService calendarPersistenceService;

  @Autowired
  private ProductCatalogService productCatalogService;

  @Autowired
  private CalendarEventRepository calendarEventRepository;

  @Autowired
  private CalendarItemSuggestionRepository calendarItemSuggestionRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void upsertsGoogleEventAndReplacesLinkedProductSuggestions() {
    long eventCount = calendarEventRepository.count();
    long suggestionCount = calendarItemSuggestionRepository.count();
    UserEntity user = createUser();
    String externalEventId = "calendar-test-" + UUID.randomUUID();
    LocalDateTime startsAt = LocalDateTime.now().plusDays(5).withNano(0);

    CalendarEventEntity first = calendarPersistenceService.upsertEvents(
        user.getUserSq(), List.of(event(externalEventId, "주말 캠핑", startsAt))).getFirst();
    CalendarEventEntity updated = calendarPersistenceService.upsertEvents(
        user.getUserSq(), List.of(event(externalEventId, "가족 캠핑", startsAt))).getFirst();

    assertThat(updated.getCalendarEventSq()).isEqualTo(first.getCalendarEventSq());
    assertThat(updated.getTitle()).isEqualTo("가족 캠핑");
    assertThat(calendarEventRepository.count()).isEqualTo(eventCount + 1);

    String providerCode = "calendar-product-" + UUID.randomUUID();
    String productUrl = "https://www.11st.co.kr/products/" + providerCode;
    productCatalogService.registerOffers(List.of(elevenstItem(providerCode, productUrl)));
    calendarPersistenceService.replaceSuggestions(updated, List.of(new CalendarSuggestionInput(
        "모기퇴치제",
        "야외 일정에 필요한 방충 준비물",
        "HIGH",
        startsAt.toLocalDate().minusDays(2),
        new ProductSnapshot(
            "테스트 모기퇴치제", "ELEVENST", providerCode, productUrl,
            "https://cdn.011st.com/calendar-product.jpg", 12_900L),
        true
    )));

    var stored = calendarPersistenceService.events(
        user.getUserSq(), startsAt.minusDays(1), startsAt.plusDays(1)).getFirst();
    assertThat(stored.suggestions()).singleElement().satisfies(item -> {
      assertThat(item.getKeyword()).isEqualTo("모기퇴치제");
      assertThat(item.getRecommendedBuyBy()).isEqualTo(startsAt.toLocalDate().minusDays(2));
      assertThat(item.getOfferSq()).isNotNull();
      assertThat(item.getProductSq()).isNotNull();
      assertThat(item.getProductLive()).isTrue();
    });

    calendarPersistenceService.replaceSuggestions(updated, List.of());
    assertThat(calendarItemSuggestionRepository.count()).isEqualTo(suggestionCount);
  }

  private CalendarEventInput event(String externalEventId, String title, LocalDateTime startsAt) {
    return new CalendarEventInput(
        "primary",
        externalEventId,
        title,
        "가족 일정",
        "가평",
        startsAt,
        startsAt.plusDays(1),
        false
    );
  }

  private ProductItem elevenstItem(String providerCode, String productUrl) {
    String image = "https://cdn.011st.com/calendar-product.jpg";
    return new ProductItem(
        "ELEVENST-" + providerCode,
        "테스트 모기퇴치제",
        12_900L,
        "테스트 판매자",
        "ELEVENST",
        image,
        productUrl,
        providerCode,
        List.of(),
        new ImageSources(image, image, image, image)
    );
  }

  private UserEntity createUser() {
    UserEntity user = new UserEntity();
    user.setEmail("calendar-test-" + UUID.randomUUID() + "@example.com");
    user.setName("calendar test");
    user.setPassword("test-password");
    user.setStatus(UserStatusCode.USE);
    return userRepository.saveAndFlush(user);
  }
}
