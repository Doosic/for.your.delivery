package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentSessionTypeCode {

  UNIFIED("UNIFIED"),
  BRIEFING("BRIEFING"),
  SHOPPING("SHOPPING"),
  CALENDAR("CALENDAR"),
  PRICE("PRICE");

  private final String type;
}
