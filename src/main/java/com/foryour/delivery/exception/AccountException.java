package com.foryour.delivery.exception;


import com.foryour.delivery.domain.enums.ErrorCode;

public class AccountException extends RuntimeException {
  public AccountException(ErrorCode errorCode) {
    super(errorCode.getMessage());
  }
}
