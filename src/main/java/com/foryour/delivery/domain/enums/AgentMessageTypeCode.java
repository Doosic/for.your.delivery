package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentMessageTypeCode {

  TEXT("TEXT"),
  BRIEFING("BRIEFING"),
  PRODUCT_LIST("PRODUCT_LIST"),
  CALENDAR_PREP("CALENDAR_PREP"),
  PRICE_ALERT("PRICE_ALERT"),
  ACTION_CONFIRMATION("ACTION_CONFIRMATION");

  private final String type;
}
