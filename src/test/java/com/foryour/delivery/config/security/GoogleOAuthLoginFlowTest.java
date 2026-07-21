package com.foryour.delivery.config.security;

import com.foryour.delivery.client.user.UserService;
import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.client.user.bean.UserSignupRequest;
import com.foryour.delivery.common.CProperties;
import com.foryour.delivery.config.jwt.JwtTokenProvider;
import com.foryour.delivery.domain.entity.UserEntity;
import com.foryour.delivery.domain.enums.UserStatusCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleOAuthLoginFlowTest {

  private CProperties properties;
  private JwtTokenProvider jwtTokenProvider;
  private RecordingUserService userService;
  private GoogleOAuth2SuccessHandler successHandler;

  @BeforeEach
  void setUp() {
    properties = new CProperties();
    properties.setFrontendBaseUrl("http://localhost:3500");
    CProperties.Jwt jwt = new CProperties.Jwt();
    jwt.setAccessHeader("DeliveryAuthAccess");
    jwt.setRefreshHeader("DeliveryAuthRefresh");
    jwt.setAccessTimeoutMin(10);
    jwt.setRefreshTimeoutMin(20);
    jwt.setSecret("test-secret-must-be-long-enough-for-hs512-signing-key-12345678901234567890");
    properties.setJwt(jwt);

    jwtTokenProvider = new JwtTokenProvider(properties);
    userService = new RecordingUserService();
    successHandler = new GoogleOAuth2SuccessHandler(userService, jwtTokenProvider, properties);
  }

  @Test
  void googleProfileCreatesSessionCookiesAndRedirectsToCompletePage() throws Exception {
    var authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
    var oauthUser = new DefaultOAuth2User(authorities, Map.of(
        "sub", "google-user-1",
        "email", "User@Example.com",
        "name", "FUB User"
    ), "email");
    var authentication = new OAuth2AuthenticationToken(oauthUser, authorities, "google");
    var response = new MockHttpServletResponse();

    successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

    assertThat(userService.email).isEqualTo("User@Example.com");
    assertThat(userService.name).isEqualTo("FUB User");
    assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3500/app/complete?provider=google");

    Cookie accessCookie = cookie(response, "DeliveryAuthAccess");
    Cookie refreshCookie = cookie(response, "DeliveryAuthRefresh");
    assertThat(accessCookie.isHttpOnly()).isTrue();
    assertThat(refreshCookie.isHttpOnly()).isTrue();
    assertThat(accessCookie.getMaxAge()).isEqualTo(600);
    assertThat(refreshCookie.getMaxAge()).isEqualTo(1_200);
    assertThat(jwtTokenProvider.parseTokenBody(accessCookie.getValue()).getEmail()).isEqualTo("user@example.com");
  }

  @Test
  void missingGoogleEmailReturnsToLoginWithReason() throws Exception {
    var authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
    var oauthUser = new DefaultOAuth2User(authorities, Map.of(
        "sub", "google-user-2",
        "name", "No Email"
    ), "sub");
    var authentication = new OAuth2AuthenticationToken(oauthUser, authorities, "google");
    var response = new MockHttpServletResponse();

    successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

    assertThat(response.getRedirectedUrl()).isEqualTo(
        "http://localhost:3500/app/login?error=google_email_missing");
    assertThat(response.getCookies()).isEmpty();
  }

  private Cookie cookie(MockHttpServletResponse response, String name) {
    return Arrays.stream(response.getCookies())
        .filter(cookie -> cookie.getName().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static class RecordingUserService implements UserService {
    private String email;
    private String name;

    @Override
    public UserResponseVO findOrCreateGoogleUser(String email, String name) {
      this.email = email;
      this.name = name;
      return UserResponseVO.builder()
          .userSq(1L)
          .email(email.trim().toLowerCase())
          .name(name)
          .status(UserStatusCode.USE)
          .build();
    }

    @Override
    public UserResponseVO getUserDetailsByEmail(String email) {
      throw new UnsupportedOperationException();
    }

    @Override
    public UserResponseVO signup(UserSignupRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public UserEntity getActiveUserByEmail(String email) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void logout(HttpServletResponse response) {
    }

    @Override
    public void increaseFailCount(String email) {
    }

    @Override
    public void resetFailCount(String email) {
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      throw new UnsupportedOperationException();
    }
  }
}
