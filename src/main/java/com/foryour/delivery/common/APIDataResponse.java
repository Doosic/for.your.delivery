package com.foryour.delivery.common;

import com.foryour.delivery.domain.enums.ErrorCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class APIDataResponse<T> extends APIErrorResponse {

  protected APIDataResponse(T data) {
    super(true, ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), data);
  }

  public static <T> APIDataResponse<T> of (T data){
    return new APIDataResponse(data);
  }
}
