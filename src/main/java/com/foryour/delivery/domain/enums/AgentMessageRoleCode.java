package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentMessageRoleCode {

  USER("USER"),
  ASSISTANT("ASSISTANT"),
  SYSTEM("SYSTEM"),
  TOOL("TOOL");

  private final String role;
}
