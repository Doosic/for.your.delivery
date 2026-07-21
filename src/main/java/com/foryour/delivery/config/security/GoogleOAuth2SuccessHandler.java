package com.foryour.delivery.config.security;

import com.foryour.delivery.client.user.UserService;
import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.common.CProperties;
import com.foryour.delivery.config.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;
  private final CProperties properties;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException {
    OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
    String email = oauthUser.getAttribute("email");
    String name = oauthUser.getAttribute("name");

    if (email == null || email.isBlank()) {
      response.sendRedirect(properties.getFrontendBaseUrl() + "/app/login?error=google_email_missing");
      return;
    }

    UserResponseVO user = userService.findOrCreateGoogleUser(email, name);
    jwtTokenProvider.issueCookies(user, response);
    response.sendRedirect(UriComponentsBuilder
        .fromUriString(properties.getFrontendBaseUrl())
        .path("/app/complete")
        .queryParam("provider", "google")
        .build(true)
        .toUriString());
  }
}
