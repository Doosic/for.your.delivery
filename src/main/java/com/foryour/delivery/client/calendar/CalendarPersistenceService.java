package com.foryour.delivery.client.calendar;

import com.foryour.delivery.domain.entity.CalendarEventEntity;
import com.foryour.delivery.domain.entity.CalendarItemSuggestionEntity;
import com.foryour.delivery.domain.entity.ProductOfferEntity;
import com.foryour.delivery.domain.enums.CalendarEventStatusCode;
import com.foryour.delivery.domain.enums.CalendarSuggestionStatusCode;
import com.foryour.delivery.domain.repository.CalendarEventRepository;
import com.foryour.delivery.domain.repository.CalendarItemSuggestionRepository;
import com.foryour.delivery.domain.repository.ProductOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarPersistenceService {

  private final CalendarEventRepository calendarEventRepository;
  private final CalendarItemSuggestionRepository calendarItemSuggestionRepository;
  private final ProductOfferRepository productOfferRepository;

  @Transactional
  public List<CalendarEventEntity> upsertEvents(Long userSq, List<CalendarEventInput> inputs) {
    LocalDateTime syncedAt = LocalDateTime.now();
    List<CalendarEventEntity> events = new ArrayList<>();
    for (CalendarEventInput input : inputs) {
      CalendarEventEntity event = calendarEventRepository
          .findByUserSqAndProviderAndProviderCalendarIdAndExternalEventId(
              userSq, "GOOGLE", input.calendarId(), input.externalEventId())
          .orElseGet(CalendarEventEntity::new);
      event.setUserSq(userSq);
      event.setProvider("GOOGLE");
      event.setProviderCalendarId(input.calendarId());
      event.setExternalEventId(input.externalEventId());
      event.setTitle(input.title());
      event.setDescription(input.description());
      event.setLocation(input.location());
      event.setStartsAt(input.startsAt());
      event.setEndsAt(input.endsAt());
      event.setAllDay(input.allDay());
      event.setStatus(CalendarEventStatusCode.ACTIVE);
      event.setLastSyncedAt(syncedAt);
      events.add(calendarEventRepository.save(event));
    }
    return events;
  }

  @Transactional
  public void replaceSuggestions(
      CalendarEventEntity event,
      List<CalendarSuggestionInput> inputs
  ) {
    calendarItemSuggestionRepository.deleteAllByCalendarEventSq(event.getCalendarEventSq());
    LocalDateTime generatedAt = LocalDateTime.now();
    for (CalendarSuggestionInput input : inputs) {
      CalendarItemSuggestionEntity suggestion = new CalendarItemSuggestionEntity();
      suggestion.setCalendarEventSq(event.getCalendarEventSq());
      suggestion.setUserSq(event.getUserSq());
      suggestion.setKeyword(input.keyword());
      suggestion.setReason(input.reason());
      suggestion.setPriority(input.priority());
      suggestion.setRecommendedBuyBy(input.recommendedBuyBy());
      applyProduct(suggestion, input.product());
      suggestion.setProductLive(input.productLive());
      suggestion.setStatus(CalendarSuggestionStatusCode.ACTIVE);
      suggestion.setGeneratedAt(generatedAt);
      calendarItemSuggestionRepository.save(suggestion);
    }
  }

  @Transactional(readOnly = true)
  public List<StoredCalendarEvent> events(Long userSq, LocalDateTime from, LocalDateTime to) {
    return calendarEventRepository
        .findAllByUserSqAndStatusAndStartsAtBetweenOrderByStartsAtAsc(
            userSq, CalendarEventStatusCode.ACTIVE, from, to)
        .stream()
        .map(event -> new StoredCalendarEvent(
            event,
            calendarItemSuggestionRepository
                .findAllByCalendarEventSqOrderByCalendarSuggestionSqAsc(event.getCalendarEventSq())))
        .toList();
  }

  private void applyProduct(
      CalendarItemSuggestionEntity suggestion,
      ProductSnapshot product
  ) {
    if (product == null) {
      return;
    }
    suggestion.setProductName(product.name());
    suggestion.setProvider(product.provider());
    suggestion.setProviderCode(product.providerCode());
    suggestion.setProductUrl(product.productUrl());
    suggestion.setImageUrl(product.imageUrl());
    suggestion.setPrice(product.price());
    productOfferRepository
        .findByProviderAndExternalProductId(product.provider(), product.providerCode())
        .ifPresent(offer -> applyOfferIds(suggestion, offer));
  }

  private void applyOfferIds(
      CalendarItemSuggestionEntity suggestion,
      ProductOfferEntity offer
  ) {
    suggestion.setProductSq(offer.getProductSq());
    suggestion.setOfferSq(offer.getOfferSq());
  }

  public record CalendarEventInput(
      String calendarId,
      String externalEventId,
      String title,
      String description,
      String location,
      LocalDateTime startsAt,
      LocalDateTime endsAt,
      boolean allDay
  ) {
  }

  public record CalendarSuggestionInput(
      String keyword,
      String reason,
      String priority,
      LocalDate recommendedBuyBy,
      ProductSnapshot product,
      boolean productLive
  ) {
  }

  public record ProductSnapshot(
      String name,
      String provider,
      String providerCode,
      String productUrl,
      String imageUrl,
      long price
  ) {
  }

  public record StoredCalendarEvent(
      CalendarEventEntity event,
      List<CalendarItemSuggestionEntity> suggestions
  ) {
  }
}
