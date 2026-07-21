package com.foryour.delivery.config.jwt;

import com.foryour.delivery.common.APIErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static com.foryour.delivery.domain.enums.ErrorCode.UNAUTHORIZED_FAIL;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException authException) throws IOException {
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    APIErrorResponse result = APIErrorResponse.of(false, UNAUTHORIZED_FAIL.getCode(), UNAUTHORIZED_FAIL.getMessage());
    final ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(response.getOutputStream(), result);

  }
}
