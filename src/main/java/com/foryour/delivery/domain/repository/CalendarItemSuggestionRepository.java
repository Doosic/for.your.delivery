package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.CalendarItemSuggestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarItemSuggestionRepository extends JpaRepository<CalendarItemSuggestionEntity, Long> {

  List<CalendarItemSuggestionEntity> findAllByCalendarEventSqOrderByCalendarSuggestionSqAsc(Long calendarEventSq);

  void deleteAllByCalendarEventSq(Long calendarEventSq);
}
