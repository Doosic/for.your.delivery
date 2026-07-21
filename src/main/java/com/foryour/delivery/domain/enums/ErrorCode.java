package com.foryour.delivery.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  OK(100,HttpStatus.OK, "ok"),

  BAD_REQUEST(800, HttpStatus.BAD_REQUEST, "bad request"),
  ACCESS_DENIED(801, HttpStatus.BAD_REQUEST, "access denied"),
  DATA_ACCESS_ERROR(802, HttpStatus.INTERNAL_SERVER_ERROR, "data access error"),
  UNAUTHORIZED_FAIL(803, HttpStatus.UNAUTHORIZED, "unauthorized error"),
  DATA_NOT_FOUND(804, HttpStatus.UNAUTHORIZED, "data not found"),

  LOGIN_FAIL(810, HttpStatus.BAD_REQUEST, "login fail"),
  LOGOUT_FAIL(811, HttpStatus.BAD_REQUEST, "logout fail"),
  DUPLICATED_DATA(812, HttpStatus.CONFLICT, "duplicated data"),
  DATA_NOT_EXIST(813, HttpStatus.BAD_REQUEST, "data not exist"),
  LOCKED_ACCOUNT(814, HttpStatus.BAD_REQUEST, "locked account"),

  PASSWORD_MISMATCH(820, HttpStatus.BAD_REQUEST, "bad credentials"),
  VALIDATION_ERROR(821, HttpStatus.BAD_REQUEST, "validation error"),

  NOT_FOUND(830,HttpStatus.BAD_REQUEST, "request resource is not found"),
  INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "internal error");

  private final Integer code;
  private final HttpStatus httpStatus;
  private final String message;

  public String getMessage(Throwable e){
    String message = this.getMessage();
//    if(e.getMessage() != null){
//      message += " - " + e.getMessage();
//    }
    return this.getMessage(message);
  }

  public String getMessage(String message) {
    return Optional.ofNullable(message)
        .filter(Predicate.not(String::isBlank))
        .orElse(this.getMessage());
  }

  public Integer getCode() {
    return this.code;
  }
}
