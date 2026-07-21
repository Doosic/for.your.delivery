package com.foryour.delivery.client.agent;

import com.foryour.delivery.client.agent.AgentService.AgentSessionView;
import com.foryour.delivery.domain.entity.UserEntity;
import com.foryour.delivery.domain.enums.UserStatusCode;
import com.foryour.delivery.domain.repository.UserRepository;
import com.foryour.delivery.domain.repository.AgentDecisionRepository;
import com.foryour.delivery.domain.repository.AgentRunRepository;
import com.foryour.delivery.exception.APIException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AgentServiceTest {

  @Autowired
  private AgentService agentService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AgentRunRepository agentRunRepository;

  @Autowired
  private AgentDecisionRepository agentDecisionRepository;

  @Test
  void createsAndReadsUnifiedSessionForOwner() {
    UserEntity owner = createUser();

    AgentSessionView created = agentService.createSession(
        owner.getUserSq(), "  오늘의 브리핑  ", Map.of("entryPoint", "BRIEFING"));
    AgentSessionView found = agentService.session(owner.getUserSq(), created.sessionSq());

    assertThat(found.sessionType()).isEqualTo("UNIFIED");
    assertThat(found.title()).isEqualTo("오늘의 브리핑");
    assertThat(found.status()).isEqualTo("ACTIVE");
    assertThat(found.context()).containsEntry("entryPoint", "BRIEFING");
    assertThat(found.messages()).isEmpty();
    assertThat(found.createdAt()).isNotNull();
  }

  @Test
  void rejectsSessionLookupByAnotherUser() {
    UserEntity owner = createUser();
    UserEntity anotherUser = createUser();
    AgentSessionView created = agentService.createSession(owner.getUserSq(), null, null);

    assertThatThrownBy(() -> agentService.session(anotherUser.getUserSq(), created.sessionSq()))
        .isInstanceOf(APIException.class)
        .hasMessage("data not exist");
  }

  @Test
  void storesUserAndAssistantMessagesWithSelectedRoute() {
    UserEntity owner = createUser();
    AgentSessionView session = agentService.createSession(owner.getUserSq(), null, null);
    long decisionCount = agentDecisionRepository.count();

    AgentService.AgentMessageResult result = agentService.sendMessage(
        owner.getUserSq(), session.sessionSq(), "다음 주 캠핑 일정에 필요한 용품을 알려줘", Map.of("locale", "ko-KR"));
    AgentSessionView found = agentService.session(owner.getUserSq(), session.sessionSq());

    assertThat(result.userMessage().text()).isEqualTo("다음 주 캠핑 일정에 필요한 용품을 알려줘");
    assertThat(result.assistantMessage().sourceAgent()).isEqualTo("CALENDAR_PREPARATION");
    assertThat(result.assistantMessage().payload())
        .containsEntry("route", "CALENDAR_PREPARATION")
        .containsEntry("dataMode", "PENDING");
    assertThat(result.userMessage().traceId()).isEqualTo(result.assistantMessage().traceId());
    assertThat(found.messages()).hasSize(2);
    assertThat(found.lastMessageAt()).isNotNull();
    assertThat(agentRunRepository
        .findAllByAgentSessionSqAndUserSqOrderByAgentRunSqAsc(session.sessionSq(), owner.getUserSq()))
        .hasSize(1);
    assertThat(agentDecisionRepository.count()).isEqualTo(decisionCount + 1);
  }

  @Test
  void masksStoredMessageAndProposesImportantWikiFact() {
    UserEntity owner = createUser();
    AgentSessionView session = agentService.createSession(owner.getUserSq(), null, null);
    long decisionCount = agentDecisionRepository.count();

    AgentService.AgentMessageResult result = agentService.sendMessage(
        owner.getUserSq(),
        session.sessionSq(),
        "나는 무료배송을 좋아해. 이메일은 lion4464@gmail.com 이야",
        Map.of("phone", "010-1234-5678")
    );

    assertThat(result.userMessage().text())
        .contains("l***@gmail.com")
        .doesNotContain("lion4464@gmail.com");
    assertThat(result.userMessage().payload()).containsEntry("phone", "010-****-5678");
    assertThat(result.assistantMessage().actions())
        .extracting(action -> action.get("type"))
        .containsExactly("CONFIRM_WIKI_ENTRY", "REJECT_WIKI_ENTRY");
    assertThat(result.assistantMessage().runSq()).isNotNull();
    assertThat(agentDecisionRepository.count()).isEqualTo(decisionCount + 2);
  }

  private UserEntity createUser() {
    UserEntity user = new UserEntity();
    user.setEmail("agent-service-test-" + UUID.randomUUID() + "@example.com");
    user.setName("agent service test");
    user.setPassword("test-password");
    user.setStatus(UserStatusCode.USE);
    return userRepository.saveAndFlush(user);
  }
}
