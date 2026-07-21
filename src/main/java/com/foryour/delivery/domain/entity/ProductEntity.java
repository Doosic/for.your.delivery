package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.ProductStatusCode;
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

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tb_fy_product")
public class ProductEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "product_sq")
  private Long productSq;

  @Column(name = "product_key", length = 255, nullable = false, unique = true)
  private String productKey;

  @Column(name = "name", length = 500, nullable = false)
  private String name;

  @Column(name = "normalized_name", length = 500, nullable = false)
  private String normalizedName;

  @Column(name = "brand", length = 200)
  private String brand;

  @Column(name = "maker", length = 200)
  private String maker;

  @Column(name = "category_path", length = 500)
  private String categoryPath;

  @Column(name = "image_url", columnDefinition = "text")
  private String imageUrl;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ProductStatusCode status = ProductStatusCode.ACTIVE;
}
