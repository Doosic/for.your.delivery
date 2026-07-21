package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.ProductOfferStatusCode;
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
@Table(name = "tb_fy_product_offer")
public class ProductOfferEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "offer_sq")
  private Long offerSq;

  @Column(name = "product_sq", nullable = false)
  private Long productSq;

  @Column(name = "provider", length = 30, nullable = false)
  private String provider;

  @Column(name = "external_product_id", length = 255, nullable = false)
  private String externalProductId;

  @Column(name = "mall_name", length = 200)
  private String mallName;

  @Column(name = "product_url", columnDefinition = "text", nullable = false)
  private String productUrl;

  @Column(name = "price", nullable = false)
  private Long price;

  @Column(name = "shipping_fee", nullable = false)
  private Long shippingFee = 0L;

  @Column(name = "total_price", nullable = false)
  private Long totalPrice;

  @Column(name = "search_rank")
  private Integer searchRank;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ProductOfferStatusCode status = ProductOfferStatusCode.ACTIVE;

  @Column(name = "last_collected_at", nullable = false)
  private LocalDateTime lastCollectedAt;
}
