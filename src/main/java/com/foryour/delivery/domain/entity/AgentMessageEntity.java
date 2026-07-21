package com.foryour.delivery.domain.entity;

import com.foryour.delivery.common.BaseTimeEntity;
import com.foryour.delivery.domain.enums.AgentMessageRoleCode;
import com.foryour.delivery.domain.enums.AgentMessageTypeCode;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tb_fy_agent_message")
public class AgentMessageEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "agent_message_sq")
  private Long agentMessageSq;

  @Column(name = "agent_session_sq", nullable = false)
  private Long agentSessionSq;

  @Column(name = "agent_run_sq")
  private Long agentRunSq;

  @Column(name = "user_sq", nullable = false)
  private Long userSq;

  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  private AgentMessageRoleCode role;

  @Column(name = "message_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private AgentMessageTypeCode messageType = AgentMessageTypeCode.TEXT;

  @Column(name = "content", columnDefinition = "text")
  private String content;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> payloadJson = new HashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "actions_json", columnDefinition = "jsonb", nullable = false)
  private List<Map<String, Object>> actionsJson = new ArrayList<>();

  @Column(name = "source_agent", length = 50)
  @Enumerated(EnumType.STRING)
  private AgentTypeCode sourceAgent;

  @Column(name = "trace_id", length = 100)
  private String traceId;
}
