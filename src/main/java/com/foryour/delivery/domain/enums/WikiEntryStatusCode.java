package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WikiEntryStatusCode {

  PENDING_CONFIRMATION("PENDING_CONFIRMATION"),
  ACTIVE("ACTIVE"),
  ARCHIVED("ARCHIVED"),
  REJECTED("REJECTED"),
  EXPIRED("EXPIRED");

  private final String status;
}
