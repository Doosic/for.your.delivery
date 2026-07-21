package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WikiSensitivityCode {

  NORMAL("NORMAL"),
  SENSITIVE("SENSITIVE");

  private final String sensitivity;
}
