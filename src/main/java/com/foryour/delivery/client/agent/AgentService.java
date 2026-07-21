package com.foryour.delivery.client.agent;

import com.foryour.delivery.client.wiki.PersonalWikiService;
import com.foryour.delivery.client.agent.AgentBriefingHarness.AgentContext;
import com.foryour.delivery.client.wiki.PersonalWikiService.WikiAgentReply;
import com.foryour.delivery.client.wiki.PersonalWikiService.WikiEntryView;
import com.foryour.delivery.common.PersonalDataMasker;
import com.foryour.delivery.domain.entity.AgentDecisionEntity;
import com.foryour.delivery.domain.entity.AgentMessageEntity;
import com.foryour.delivery.domain.entity.AgentRunEntity;
import com.foryour.delivery.domain.entity.AgentSessionEntity;
import com.foryour.delivery.domain.enums.AgentDecisionStatusCode;
import com.foryour.delivery.domain.enums.AgentRunStatusCode;
import com.foryour.delivery.domain.enums.AgentSessionStatusCode;
import com.foryour.delivery.domain.enums.AgentSessionTypeCode;
import com.foryour.delivery.domain.enums.AgentMessageRoleCode;
import com.foryour.delivery.domain.enums.AgentMessageTypeCode;
import com.foryour.delivery.domain.enums.AgentTypeCode;
import com.foryour.delivery.domain.repository.AgentMessageRepository;
import com.foryour.delivery.domain.repository.AgentDecisionRepository;
import com.foryour.delivery.domain.repository.AgentRunRepository;
import com.foryour.delivery.domain.repository.AgentSessionRepository;
import com.foryour.delivery.exception.APIException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.foryour.delivery.domain.enums.ErrorCode.DATA_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class AgentService {

  private final AgentSessionRepository agentSessionRepository;
  private final AgentMessageRepository agentMessageRepository;
  private final AgentRunRepository agentRunRepository;
  private final AgentDecisionRepository agentDecisionRepository;
  private final PersonalWikiService personalWikiService;
  private final PersonalDataMasker personalDataMasker;
  private final OpenAiAgentClient openAiAgentClient;
  private final AgentBriefingHarness agentBriefingHarness;

  @Transactional
  public AgentSessionView createSession(Long userSq, String title, Map<String, Object> context) {
    AgentSessionEntity session = new AgentSessionEntity();
    session.setUserSq(userSq);
    session.setSessionType(AgentSessionTypeCode.UNIFIED);
    session.setTitle(normalizeTitle(title));
    session.setStatus(AgentSessionStatusCode.ACTIVE);
    session.setContextJson(context == null ? new HashMap<>() : new HashMap<>(context));

    return toView(agentSessionRepository.save(session), List.of());
  }

  @Transactional(readOnly = true)
  public AgentSessionView session(Long userSq, Long sessionSq) {
    Optional<AgentSessionEntity> sessionOptional =
        agentSessionRepository.findByAgentSessionSqAndUserSq(sessionSq, userSq);
    if (sessionOptional.isEmpty()) {
      throw new APIException(DATA_NOT_EXIST);
    }
    AgentSessionEntity session = sessionOptional.get();
    List<AgentMessageEntity> messages = agentMessageRepository
        .findAllByAgentSessionSqAndUserSqOrderByAgentMessageSqAsc(sessionSq, userSq);

    return toView(session, messages);
  }

  @Transactional
  public AgentMessageResult sendMessage(
      Long userSq,
      Long sessionSq,
      String text,
      Map<String, Object> context
  ) {
    Optional<AgentSessionEntity> sessionOptional =
        agentSessionRepository.findByAgentSessionSqAndUserSq(sessionSq, userSq);
    if (sessionOptional.isEmpty()
        || !AgentSessionStatusCode.ACTIVE.equals(sessionOptional.get().getStatus())) {
      throw new APIException(DATA_NOT_EXIST);
    }
    AgentSessionEntity session = sessionOptional.get();

    String normalizedText = personalDataMasker.mask(text.trim());
    Map<String, Object> maskedContext = personalDataMasker.mask(context);
    String traceId = "agent-" + userSq + "-" + UUID.randomUUID();
    LocalDateTime messageAt = LocalDateTime.now();

    AgentMessageEntity userMessage = new AgentMessageEntity();
    userMessage.setAgentSessionSq(sessionSq);
    userMessage.setUserSq(userSq);
    userMessage.setRole(AgentMessageRoleCode.USER);
    userMessage.setMessageType(AgentMessageTypeCode.TEXT);
    userMessage.setContent(normalizedText);
    userMessage.setPayloadJson(maskedContext);
    userMessage.setTraceId(traceId);
    userMessage = agentMessageRepository.save(userMessage);

    AgentTypeCode route = route(normalizedText);
    AgentRunEntity run = createRun(userSq, sessionSq, userMessage.getAgentMessageSq(), route, traceId,
        normalizedText.length(), maskedContext.size(), messageAt);
    AgentReply reply = createReply(
        userSq,
        userMessage.getAgentMessageSq(),
        route,
        normalizedText,
        personalDataMasker.mask(session.getContextJson()),
        maskedContext
    );

    AgentMessageEntity assistantMessage = new AgentMessageEntity();
    assistantMessage.setAgentSessionSq(sessionSq);
    assistantMessage.setAgentRunSq(run.getAgentRunSq());
    assistantMessage.setUserSq(userSq);
    assistantMessage.setRole(AgentMessageRoleCode.ASSISTANT);
    assistantMessage.setMessageType(reply.messageType());
    assistantMessage.setContent(reply.text());
    assistantMessage.setPayloadJson(reply.payload());
    assistantMessage.setActionsJson(reply.actions());
    assistantMessage.setSourceAgent(route);
    assistantMessage.setTraceId(traceId);
    assistantMessage = agentMessageRepository.save(assistantMessage);

    saveRoutingDecision(run, route);
    if (reply.candidateWikiEntrySq() != null) {
      saveWikiDecision(run, reply.candidateWikiEntrySq());
    }
    run.setModelName(reply.modelName());
    run.setPromptVersion(reply.promptVersion());
    run.setStatus(AgentRunStatusCode.SUCCEEDED);
    run.setOutputSummaryJson(Map.of(
        "messageType", reply.messageType().name(),
        "actionCount", reply.actions().size(),
        "hasWikiCandidate", reply.candidateWikiEntrySq() != null,
        "generationMode", reply.payload().get("generationMode")
    ));
    run.setCompletedAt(LocalDateTime.now());
    agentRunRepository.save(run);

    session.setLastMessageAt(messageAt);
    agentSessionRepository.save(session);

    return new AgentMessageResult(
        sessionSq,
        toMessageView(userMessage),
        toMessageView(assistantMessage)
    );
  }

  private String normalizeTitle(String title) {
    return title == null || title.isBlank() ? "새 대화" : title.trim();
  }

  private AgentSessionView toView(AgentSessionEntity session, List<AgentMessageEntity> messages) {
    List<AgentMessageView> messageViews = new java.util.ArrayList<>();
    for (AgentMessageEntity message : messages) {
      messageViews.add(toMessageView(message));
    }
    return new AgentSessionView(
        session.getAgentSessionSq(),
        session.getSessionType().name(),
        session.getTitle(),
        session.getStatus().name(),
        session.getContextJson(),
        messageViews,
        session.getLastMessageAt(),
        session.getCreateDate(),
        session.getModifiedDate()
    );
  }

  private AgentMessageView toMessageView(AgentMessageEntity message) {
    return new AgentMessageView(
        message.getAgentMessageSq(),
        message.getAgentRunSq(),
        message.getRole().name(),
        message.getMessageType().name(),
        message.getContent(),
        message.getPayloadJson(),
        message.getActionsJson(),
        message.getSourceAgent() == null ? null : message.getSourceAgent().name(),
        message.getTraceId(),
        message.getCreateDate()
    );
  }

  private AgentTypeCode route(String text) {
    String normalized = text.toLowerCase(Locale.ROOT);
    if (containsAny(normalized, "위키", "기억해", "기억해줘", "내 정보", "내 취향", "remember")) {
      return AgentTypeCode.PERSONAL_WIKI;
    }
    boolean priceIntent = containsAny(
        normalized, "가격", "최저가", "최적가", "할인", "가격하락", "구매 시점", "언제 주문", "언제 사", "price");
    boolean calendarIntent = containsAny(
        normalized, "일정", "캘린더", "여행", "캠핑", "행사", "calendar");
    if (priceIntent && calendarIntent) {
      return AgentTypeCode.BRIEFING_SHOPPING;
    }
    if (priceIntent) {
      return AgentTypeCode.PRICE_INTELLIGENCE;
    }
    if (calendarIntent) {
      return AgentTypeCode.CALENDAR_PREPARATION;
    }
    if (containsAny(normalized, "구매", "상품", "쇼핑", "추천", "브리핑", "사료", "간식", "모래", "용품")) {
      return AgentTypeCode.BRIEFING_SHOPPING;
    }
    return AgentTypeCode.CONVERSATION_ORCHESTRATOR;
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  private String responseText(AgentTypeCode route, AgentContext context) {
    if (context != null && !context.recommendations().isEmpty()) {
      int recommendationCount = context.recommendations().size();
      return switch (route) {
        case BRIEFING_SHOPPING -> "일정과 최근 30일 가격을 함께 비교해 "
            + recommendationCount + "개 상품의 구매 시점을 정리했어요.";
        case CALENDAR_PREPARATION -> "다가오는 일정에서 필요한 상품 "
            + recommendationCount + "개와 구매 마감일을 정리했어요.";
        case PRICE_INTELLIGENCE -> "최근 30일 가격 이력으로 "
            + recommendationCount + "개 상품의 현재 가격과 다음 확인일을 비교했어요.";
        default -> "요청을 확인했어요.";
      };
    }
    switch (route) {
      case BRIEFING_SHOPPING:
        return "쇼핑 요청을 확인했어요. 상품과 재고 데이터를 연결해 추천을 준비할게요.";
      case CALENDAR_PREPARATION:
        return "일정 준비 요청을 확인했어요. 연결된 일정에서 필요한 품목을 분석할 준비가 됐어요.";
      case PRICE_INTELLIGENCE:
        return "가격 분석 요청을 확인했어요. 가격 이력에서 최저가와 구매 시점을 비교할게요.";
      case PERSONAL_WIKI:
        return "개인 위키 요청을 확인했어요.";
      case CONVERSATION_ORCHESTRATOR:
        return "요청을 확인했어요. 필요한 정보를 이어서 알려주세요.";
      default:
        throw new IllegalArgumentException("Unsupported route: " + route);
    }
  }

  private AgentReply createReply(
      Long userSq,
      Long messageSq,
      AgentTypeCode route,
      String text,
      Map<String, Object> sessionContext,
      Map<String, Object> requestContext
  ) {
    AgentReply deterministicReply;
    if (AgentTypeCode.PERSONAL_WIKI.equals(route)) {
      WikiAgentReply reply = personalWikiService.respondToChat(userSq, messageSq, text);
      Map<String, Object> payload = new HashMap<>(reply.payload());
      payload.put("route", route.name());
      payload.put("status", "ACCEPTED");
      payload.put("dataMode", "PERSISTED");
      deterministicReply = new AgentReply(
          reply.messageType(), reply.text(), payload, reply.actions(), reply.candidateWikiEntrySq(),
          "RULE_BASED", "v1.1");
      return enhanceReply(route, text, sessionContext, requestContext, deterministicReply);
    }

    AgentContext agentContext = agentBriefingHarness.contextFor(userSq, route, text);
    if (agentContext == null) {
      agentContext = AgentContext.empty();
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("route", route.name());
    payload.put("status", "ACCEPTED");
    payload.put("dataMode", agentContext.dataMode());
    payload.put("recommendations", agentContext.recommendations());
    payload.put("evidencePolicy", agentContext.evidencePolicy());
    if (agentContext.briefing() != null) {
      payload.put("briefing", agentContext.briefing());
    }
    List<Map<String, Object>> actions = new java.util.ArrayList<>();
    WikiEntryView candidate = personalWikiService.proposeImportantFact(userSq, messageSq, text);
    Long candidateSq = null;
    if (candidate != null) {
      candidateSq = candidate.wikiEntrySq();
      payload.put("wikiCandidate", candidate);
      actions.add(Map.of("type", "CONFIRM_WIKI_ENTRY", "wikiEntrySq", candidateSq));
      actions.add(Map.of("type", "REJECT_WIKI_ENTRY", "wikiEntrySq", candidateSq));
    }
    deterministicReply = new AgentReply(
        AgentMessageTypeCode.TEXT,
        responseText(route, agentContext),
        payload,
        actions,
        candidateSq,
        "RULE_BASED",
        "v2.1"
    );
    return enhanceReply(route, text, sessionContext, requestContext, deterministicReply);
  }

  private AgentReply enhanceReply(
      AgentTypeCode route,
      String text,
      Map<String, Object> sessionContext,
      Map<String, Object> requestContext,
      AgentReply deterministicReply
  ) {
    Map<String, Object> safeContext = new LinkedHashMap<>();
    safeContext.put("userMessage", text);
    safeContext.put("sessionContext", sessionContext);
    safeContext.put("requestContext", requestContext);
    safeContext.put("responseType", deterministicReply.messageType().name());
    safeContext.put("draftResponse", deterministicReply.text());
    Map<String, Object> resultSummary = new LinkedHashMap<>();
    for (String key : List.of(
        "route", "status", "dataMode", "recommendations", "evidencePolicy", "wikiCandidate")) {
      if (deterministicReply.payload().containsKey(key)) {
        resultSummary.put(key, deterministicReply.payload().get(key));
      }
    }
    safeContext.put("resultSummary", personalDataMasker.mask(resultSummary));

    return openAiAgentClient.generate(route, personalDataMasker.mask(safeContext))
        .map(generated -> {
          Map<String, Object> payload = new HashMap<>(deterministicReply.payload());
          payload.put("generationMode", "OPENAI");
          payload.put("model", generated.model());
          if (generated.responseId() != null) {
            payload.put("responseId", generated.responseId());
          }
          return new AgentReply(
              deterministicReply.messageType(),
              generated.text(),
              payload,
              deterministicReply.actions(),
              deterministicReply.candidateWikiEntrySq(),
              generated.model(),
              "openai-briefing-v2.1"
          );
        })
        .orElseGet(() -> {
          Map<String, Object> payload = new HashMap<>(deterministicReply.payload());
          payload.put("generationMode", "RULE_BASED");
          return new AgentReply(
              deterministicReply.messageType(),
              deterministicReply.text(),
              payload,
              deterministicReply.actions(),
              deterministicReply.candidateWikiEntrySq(),
              deterministicReply.modelName(),
              deterministicReply.promptVersion()
          );
        });
  }

  private AgentRunEntity createRun(
      Long userSq,
      Long sessionSq,
      Long triggerMessageSq,
      AgentTypeCode route,
      String traceId,
      int messageLength,
      int contextKeyCount,
      LocalDateTime startedAt
  ) {
    AgentRunEntity run = new AgentRunEntity();
    run.setUserSq(userSq);
    run.setAgentSessionSq(sessionSq);
    run.setTriggerMessageSq(triggerMessageSq);
    run.setAgentType(route);
    run.setStatus(AgentRunStatusCode.RUNNING);
    run.setInputSummaryJson(Map.of(
        "messageLength", messageLength,
        "contextKeyCount", contextKeyCount,
        "route", route.name()
    ));
    run.setModelName("RULE_BASED");
    run.setPromptVersion("v2.1");
    run.setStartedAt(startedAt);
    run.setTraceId(traceId);
    return agentRunRepository.save(run);
  }

  private void saveRoutingDecision(AgentRunEntity run, AgentTypeCode route) {
    AgentDecisionEntity decision = new AgentDecisionEntity();
    decision.setAgentRunSq(run.getAgentRunSq());
    decision.setUserSq(run.getUserSq());
    decision.setDecisionType("ROUTING");
    decision.setDecision(route.name());
    decision.setScore(new BigDecimal("0.70000"));
    decision.setReasonJson(List.of(Map.of(
        "code", "KEYWORD_ROUTE",
        "description", "메시지 키워드 정책에 따라 담당 Agent를 선택"
    )));
    decision.setEvidenceJson(Map.of("policyVersion", "v1.1"));
    decision.setStatus(AgentDecisionStatusCode.ACTIVE);
    agentDecisionRepository.save(decision);
  }

  private void saveWikiDecision(AgentRunEntity run, Long wikiEntrySq) {
    AgentDecisionEntity decision = new AgentDecisionEntity();
    decision.setAgentRunSq(run.getAgentRunSq());
    decision.setUserSq(run.getUserSq());
    decision.setDecisionType("WIKI_UPDATE");
    decision.setDecision("PENDING_CONFIRMATION");
    decision.setScore(new BigDecimal("0.70000"));
    decision.setTargetType("WIKI_ENTRY");
    decision.setTargetSq(wikiEntrySq);
    decision.setReasonJson(List.of(Map.of(
        "code", "IMPORTANT_FACT_CANDIDATE",
        "description", "개인화에 지속적으로 활용할 수 있는 사용자 정보 후보"
    )));
    decision.setEvidenceJson(Map.of("requiresUserConfirmation", true));
    decision.setStatus(AgentDecisionStatusCode.ACTIVE);
    agentDecisionRepository.save(decision);
  }

  public record AgentMessageResult(
      Long sessionSq,
      AgentMessageView userMessage,
      AgentMessageView assistantMessage
  ) {
  }

  public record AgentSessionView(
      Long sessionSq,
      String sessionType,
      String title,
      String status,
      Map<String, Object> context,
      List<AgentMessageView> messages,
      LocalDateTime lastMessageAt,
      LocalDateTime createdAt,
      LocalDateTime modifiedAt
  ) {
  }

  public record AgentMessageView(
      Long messageSq,
      Long runSq,
      String role,
      String type,
      String text,
      Map<String, Object> payload,
      List<Map<String, Object>> actions,
      String sourceAgent,
      String traceId,
      LocalDateTime createdAt
  ) {
  }

  private record AgentReply(
      AgentMessageTypeCode messageType,
      String text,
      Map<String, Object> payload,
      List<Map<String, Object>> actions,
      Long candidateWikiEntrySq,
      String modelName,
      String promptVersion
  ) {
  }
}
