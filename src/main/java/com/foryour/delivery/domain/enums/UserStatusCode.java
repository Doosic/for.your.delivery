package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatusCode {

  DELETE("DELETE"),
  LOCK("LOCK"),
  USE("USE");

  private final String status;
}
