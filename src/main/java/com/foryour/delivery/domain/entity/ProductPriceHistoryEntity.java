package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "tb_fy_product_price_history")
public class ProductPriceHistoryEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "price_history_sq")
  private Long priceHistorySq;

  @Column(name = "offer_sq", nullable = false)
  private Long offerSq;

  @Column(name = "price", nullable = false)
  private Long price;

  @Column(name = "shipping_fee", nullable = false)
  private Long shippingFee = 0L;

  @Column(name = "total_price", nullable = false)
  private Long totalPrice;

  @Column(name = "collected_at", nullable = false)
  private LocalDateTime collectedAt;
}
