package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.AgentRunStatusCode;
import com.foryour.delivery.domain.enums.AgentTypeCode;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tb_fy_agent_run")
public class AgentRunEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "agent_run_sq")
  private Long agentRunSq;

  @Column(name = "user_sq", nullable = false)
  private Long userSq;

  @Column(name = "agent_session_sq", nullable = false)
  private Long agentSessionSq;

  @Column(name = "trigger_message_sq")
  private Long triggerMessageSq;

  @Column(name = "agent_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private AgentTypeCode agentType;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private AgentRunStatusCode status = AgentRunStatusCode.RUNNING;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "input_summary_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> inputSummaryJson = new HashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "output_summary_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> outputSummaryJson = new HashMap<>();

  @Column(name = "model_name", length = 100)
  private String modelName;

  @Column(name = "prompt_version", length = 30, nullable = false)
  private String promptVersion;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "error_code", length = 50)
  private String errorCode;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  @Column(name = "trace_id", length = 100, nullable = false)
  private String traceId;
}
