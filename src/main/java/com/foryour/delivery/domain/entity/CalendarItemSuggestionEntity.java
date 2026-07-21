package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.CalendarSuggestionStatusCode;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tb_fy_calendar_item_suggestion")
public class CalendarItemSuggestionEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "calendar_suggestion_sq")
  private Long calendarSuggestionSq;

  @Column(name = "calendar_event_sq", nullable = false)
  private Long calendarEventSq;

  @Column(name = "user_sq", nullable = false)
  private Long userSq;

  @Column(name = "keyword", length = 200, nullable = false)
  private String keyword;

  @Column(name = "reason", length = 500, nullable = false)
  private String reason;

  @Column(name = "priority", length = 20, nullable = false)
  private String priority;

  @Column(name = "recommended_buy_by", nullable = false)
  private LocalDate recommendedBuyBy;

  @Column(name = "product_sq")
  private Long productSq;

  @Column(name = "offer_sq")
  private Long offerSq;

  @Column(name = "product_name", length = 500)
  private String productName;

  @Column(name = "provider", length = 30)
  private String provider;

  @Column(name = "provider_code", length = 255)
  private String providerCode;

  @Column(name = "product_url", columnDefinition = "text")
  private String productUrl;

  @Column(name = "image_url", columnDefinition = "text")
  private String imageUrl;

  @Column(name = "price")
  private Long price;

  @Column(name = "product_live", nullable = false)
  private Boolean productLive = false;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private CalendarSuggestionStatusCode status = CalendarSuggestionStatusCode.ACTIVE;

  @Column(name = "generated_at", nullable = false)
  private LocalDateTime generatedAt;
}
