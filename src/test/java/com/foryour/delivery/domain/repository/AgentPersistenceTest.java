package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.AgentMessageEntity;
import com.foryour.delivery.domain.entity.AgentSessionEntity;
import com.foryour.delivery.domain.entity.UserEntity;
import com.foryour.delivery.domain.enums.AgentMessageRoleCode;
import com.foryour.delivery.domain.enums.AgentMessageTypeCode;
import com.foryour.delivery.domain.enums.AgentSessionStatusCode;
import com.foryour.delivery.domain.enums.AgentSessionTypeCode;
import com.foryour.delivery.domain.enums.AgentTypeCode;
import com.foryour.delivery.domain.enums.UserStatusCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AgentPersistenceTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AgentSessionRepository agentSessionRepository;

  @Autowired
  private AgentMessageRepository agentMessageRepository;

  @Test
  void savesAndReadsUserScopedAgentConversation() {
    UserEntity user = new UserEntity();
    user.setEmail("agent-test-" + UUID.randomUUID() + "@example.com");
    user.setName("agent persistence test");
    user.setPassword("test-password");
    user.setStatus(UserStatusCode.USE);
    user = userRepository.saveAndFlush(user);

    AgentSessionEntity session = new AgentSessionEntity();
    session.setUserSq(user.getUserSq());
    session.setSessionType(AgentSessionTypeCode.UNIFIED);
    session.setTitle("오늘의 브리핑");
    session.setStatus(AgentSessionStatusCode.ACTIVE);
    session.setContextJson(Map.of("entryPoint", "BRIEFING"));
    session.setLastMessageAt(LocalDateTime.now());
    session = agentSessionRepository.saveAndFlush(session);

    AgentMessageEntity message = new AgentMessageEntity();
    message.setAgentSessionSq(session.getAgentSessionSq());
    message.setUserSq(user.getUserSq());
    message.setRole(AgentMessageRoleCode.ASSISTANT);
    message.setMessageType(AgentMessageTypeCode.BRIEFING);
    message.setContent("오늘의 구매 브리핑입니다.");
    message.setPayloadJson(Map.of("sections", List.of()));
    message.setActionsJson(List.of(Map.of("type", "OPEN_PRODUCTS")));
    message.setSourceAgent(AgentTypeCode.BRIEFING_SHOPPING);
    message.setTraceId("agent-persistence-test");
    agentMessageRepository.saveAndFlush(message);

    AgentSessionEntity savedSession = agentSessionRepository
        .findByAgentSessionSqAndUserSq(session.getAgentSessionSq(), user.getUserSq())
        .orElseThrow();
    List<AgentMessageEntity> savedMessages = agentMessageRepository
        .findAllByAgentSessionSqAndUserSqOrderByAgentMessageSqAsc(
            session.getAgentSessionSq(), user.getUserSq());

    assertThat(savedSession.getContextJson()).containsEntry("entryPoint", "BRIEFING");
    assertThat(savedSession.getCreateDate()).isNotNull();
    assertThat(savedMessages).hasSize(1);
    assertThat(savedMessages.getFirst().getMessageType()).isEqualTo(AgentMessageTypeCode.BRIEFING);
    assertThat(savedMessages.getFirst().getActionsJson().getFirst()).containsEntry("type", "OPEN_PRODUCTS");
    assertThat(savedMessages.getFirst().getCreateDate()).isNotNull();
  }
}
