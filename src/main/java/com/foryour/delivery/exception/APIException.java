package com.foryour.delivery.exception;

import com.foryour.delivery.domain.enums.ErrorCode;
import lombok.Getter;

@Getter
public class APIException extends RuntimeException{

  public APIException(ErrorCode errorCode) {
    super(errorCode.getMessage());
  }
}
