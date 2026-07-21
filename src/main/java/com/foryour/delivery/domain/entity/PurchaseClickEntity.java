package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.PurchaseClickSourceCode;
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
@Table(name = "tb_fy_purchase_click")
public class PurchaseClickEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "purchase_click_sq")
  private Long purchaseClickSq;

  @Column(name = "user_sq")
  private Long userSq;

  @Column(name = "product_sq", nullable = false)
  private Long productSq;

  @Column(name = "offer_sq", nullable = false)
  private Long offerSq;

  @Column(name = "provider", length = 30, nullable = false)
  private String provider;

  @Column(name = "external_product_id", length = 255, nullable = false)
  private String externalProductId;

  @Column(name = "target_url", columnDefinition = "text", nullable = false)
  private String targetUrl;

  @Column(name = "price_at_click", nullable = false)
  private Long priceAtClick;

  @Column(name = "source_context", length = 30, nullable = false)
  @Enumerated(EnumType.STRING)
  private PurchaseClickSourceCode sourceContext = PurchaseClickSourceCode.OTHER;

  @Column(name = "clicked_at", nullable = false)
  private LocalDateTime clickedAt;
}
