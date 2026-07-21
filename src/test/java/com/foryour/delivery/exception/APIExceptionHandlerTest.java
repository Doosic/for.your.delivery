package com.foryour.delivery.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static com.foryour.delivery.domain.enums.ErrorCode.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class APIExceptionHandlerTest {

  @Test
  void mapsApiBadRequestToHttpBadRequest() {
    APIExceptionHandler handler = new APIExceptionHandler();

    ResponseEntity<Object> response = handler.handleGlobalExceptionAPI(
        new APIException(BAD_REQUEST), mock(WebRequest.class));

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST.getHttpStatus());
  }
}
