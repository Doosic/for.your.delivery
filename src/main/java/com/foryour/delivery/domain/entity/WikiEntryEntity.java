package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.WikiEntryStatusCode;
import com.foryour.delivery.domain.enums.WikiSensitivityCode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tb_fy_wiki_entry")
public class WikiEntryEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "wiki_entry_sq")
  private Long wikiEntrySq;

  @Column(name = "user_sq", nullable = false)
  private Long userSq;

  @Column(name = "category", length = 30, nullable = false)
  private String category;

  @Column(name = "entry_key", length = 100, nullable = false)
  private String entryKey;

  @Column(name = "summary", length = 500, nullable = false)
  private String summary;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "content_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> contentJson = new HashMap<>();

  @Column(name = "source_type", length = 30, nullable = false)
  private String sourceType;

  @Column(name = "source_ref_sq")
  private Long sourceRefSq;

  @Column(name = "confidence", precision = 5, scale = 4, nullable = false)
  private BigDecimal confidence;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private WikiEntryStatusCode status;

  @Column(name = "sensitivity_level", nullable = false)
  @Enumerated(EnumType.STRING)
  private WikiSensitivityCode sensitivityLevel = WikiSensitivityCode.NORMAL;

  @Column(name = "version", nullable = false)
  private Integer version = 1;

  @Column(name = "valid_until")
  private LocalDate validUntil;
}
