package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductOfferStatusCode {

  ACTIVE("ACTIVE"),
  UNAVAILABLE("UNAVAILABLE");

  private final String status;
}
