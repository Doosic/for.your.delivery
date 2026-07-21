package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.CalendarEventStatusCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tb_fy_calendar_event")
public class CalendarEventEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "calendar_event_sq")
  private Long calendarEventSq;

  @Column(name = "user_sq", nullable = false)
  private Long userSq;

  @Column(name = "provider", length = 30, nullable = false)
  private String provider;

  @Column(name = "provider_calendar_id", length = 255, nullable = false)
  private String providerCalendarId;

  @Column(name = "external_event_id", length = 255, nullable = false)
  private String externalEventId;

  @Column(name = "title", length = 500, nullable = false)
  private String title;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "location", length = 500)
  private String location;

  @Column(name = "starts_at", nullable = false)
  private LocalDateTime startsAt;

  @Column(name = "ends_at", nullable = false)
  private LocalDateTime endsAt;

  @Column(name = "all_day", nullable = false)
  private Boolean allDay = false;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private CalendarEventStatusCode status = CalendarEventStatusCode.ACTIVE;

  @Column(name = "last_synced_at", nullable = false)
  private LocalDateTime lastSyncedAt;
}
