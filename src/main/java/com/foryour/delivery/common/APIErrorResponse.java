package com.foryour.delivery.common;

import lombok.*;

@Getter
@EqualsAndHashCode
@ToString
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class APIErrorResponse<T> {

  private final Boolean success;
  private final Integer statusCode;
  private final String message;
  private final T data;

  public static APIErrorResponse of(Boolean success, Integer errorCode, String message){
    return new APIErrorResponse(success, errorCode, message, null);
  }
}
