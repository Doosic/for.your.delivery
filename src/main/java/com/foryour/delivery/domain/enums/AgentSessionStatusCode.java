package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentSessionStatusCode {

  ACTIVE("ACTIVE"),
  CLOSED("CLOSED");

  private final String status;
}
