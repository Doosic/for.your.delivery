package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.AgentSessionStatusCode;
import com.foryour.delivery.domain.enums.AgentSessionTypeCode;
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
@Table(name = "tb_fy_agent_session")
public class AgentSessionEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "agent_session_sq")
  private Long agentSessionSq;

  @Column(name = "user_sq", nullable = false)
  private Long userSq;

  @Column(name = "session_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private AgentSessionTypeCode sessionType = AgentSessionTypeCode.UNIFIED;

  @Column(name = "title", length = 200)
  private String title;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private AgentSessionStatusCode status = AgentSessionStatusCode.ACTIVE;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "context_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> contextJson = new HashMap<>();

  @Column(name = "last_message_at")
  private LocalDateTime lastMessageAt;
}
