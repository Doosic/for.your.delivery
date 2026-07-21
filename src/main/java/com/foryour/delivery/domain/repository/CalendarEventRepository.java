package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.CalendarEventEntity;
import com.foryour.delivery.domain.enums.CalendarEventStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {

  Optional<CalendarEventEntity> findByUserSqAndProviderAndProviderCalendarIdAndExternalEventId(
      Long userSq,
      String provider,
      String providerCalendarId,
      String externalEventId
  );

  List<CalendarEventEntity> findAllByUserSqAndStatusAndStartsAtBetweenOrderByStartsAtAsc(
      Long userSq,
      CalendarEventStatusCode status,
      LocalDateTime from,
      LocalDateTime to
  );
}
