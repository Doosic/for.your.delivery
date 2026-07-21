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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tb_fy_wiki_entry_history")
public class WikiEntryHistoryEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "wiki_history_sq")
  private Long wikiHistorySq;

  @Column(name = "wiki_entry_sq", nullable = false)
  private Long wikiEntrySq;

  @Column(name = "user_sq", nullable = false)
  private Long userSq;

  @Column(name = "version", nullable = false)
  private Integer version;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "snapshot_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> snapshotJson = new HashMap<>();

  @Column(name = "changed_by", length = 30, nullable = false)
  private String changedBy;

  @Column(name = "change_reason", length = 200)
  private String changeReason;
}
