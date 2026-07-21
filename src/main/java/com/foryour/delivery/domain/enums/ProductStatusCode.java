package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatusCode {

  ACTIVE("ACTIVE"),
  INACTIVE("INACTIVE");

  private final String status;
}
