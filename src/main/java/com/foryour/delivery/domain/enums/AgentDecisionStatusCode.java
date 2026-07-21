package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentDecisionStatusCode {

  ACTIVE("ACTIVE"),
  SUPERSEDED("SUPERSEDED"),
  EXPIRED("EXPIRED");

  private final String status;
}
