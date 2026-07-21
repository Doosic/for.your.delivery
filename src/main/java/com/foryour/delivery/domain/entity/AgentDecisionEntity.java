package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.AgentDecisionStatusCode;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tb_fy_agent_decision")
public class AgentDecisionEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "agent_decision_sq")
  private Long agentDecisionSq;

  @Column(name = "agent_run_sq", nullable = false)
  private Long agentRunSq;

  @Column(name = "user_sq", nullable = false)
  private Long userSq;

  @Column(name = "decision_type", length = 30, nullable = false)
  private String decisionType;

  @Column(name = "decision", length = 50, nullable = false)
  private String decision;

  @Column(name = "score", precision = 6, scale = 5)
  private BigDecimal score;

  @Column(name = "target_type", length = 30)
  private String targetType;

  @Column(name = "target_sq")
  private Long targetSq;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "reason_json", columnDefinition = "jsonb", nullable = false)
  private List<Map<String, Object>> reasonJson = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "evidence_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> evidenceJson = Map.of();

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private AgentDecisionStatusCode status = AgentDecisionStatusCode.ACTIVE;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;
}
