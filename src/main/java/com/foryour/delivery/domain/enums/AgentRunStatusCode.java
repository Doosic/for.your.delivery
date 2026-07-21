package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentRunStatusCode {

  RUNNING("RUNNING"),
  SUCCEEDED("SUCCEEDED"),
  FAILED("FAILED");

  private final String status;
}
