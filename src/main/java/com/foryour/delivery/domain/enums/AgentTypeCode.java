package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentTypeCode {

  CONVERSATION_ORCHESTRATOR("CONVERSATION_ORCHESTRATOR"),
  BRIEFING_SHOPPING("BRIEFING_SHOPPING"),
  CALENDAR_PREPARATION("CALENDAR_PREPARATION"),
  PRICE_INTELLIGENCE("PRICE_INTELLIGENCE"),
  PERSONAL_WIKI("PERSONAL_WIKI");

  private final String type;
}
