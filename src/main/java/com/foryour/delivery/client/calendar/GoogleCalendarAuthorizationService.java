package com.foryour.delivery.client.calendar;

import com.foryour.delivery.common.CProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class GoogleCalendarAuthorizationService {

  static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events.readonly";
  static final String SESSION_STATE = "FUB_GOOGLE_CALENDAR_OAUTH_STATE";
  static final String SESSION_USER_EMAIL = "FUB_GOOGLE_CALENDAR_USER_EMAIL";

  private final ClientRegistrationRepository clientRegistrationRepository;
  private final OAuth2AuthorizedClientService authorizedClientService;
  private final CProperties properties;
  private final RestClient restClient;

  @Autowired
  public GoogleCalendarAuthorizationService(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService authorizedClientService,
      CProperties properties
  ) {
    this(clientRegistrationRepository, authorizedClientService, properties, RestClient.create());
  }

  GoogleCalendarAuthorizationService(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService authorizedClientService,
      CProperties properties,
      RestClient restClient
  ) {
    this.clientRegistrationRepository = clientRegistrationRepository;
    this.authorizedClientService = authorizedClientService;
    this.properties = properties;
    this.restClient = restClient;
  }

  public String authorizationUrl(String fubUserEmail, HttpServletRequest request) {
    ClientRegistration registration = googleRegistration();
    String state = UUID.randomUUID().toString();
    HttpSession session = request.getSession(true);
    session.setAttribute(SESSION_STATE, state);
    session.setAttribute(SESSION_USER_EMAIL, fubUserEmail);

    return UriComponentsBuilder
        .fromUriString(registration.getProviderDetails().getAuthorizationUri())
        .queryParam("client_id", registration.getClientId())
        .queryParam("redirect_uri", properties.getGoogleCalendar().getRedirectUri())
        .queryParam("response_type", "code")
        .queryParam("scope", CALENDAR_SCOPE)
        .queryParam("access_type", "offline")
        .queryParam("prompt", "consent")
        .queryParam("include_granted_scopes", "true")
        .queryParam("state", state)
        .build()
        .encode()
        .toUriString();
  }

  public String complete(String code, String state, String oauthError, HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (oauthError != null || session == null || !stateMatches(session, state)) {
      clearSession(session);
      return frontendRedirect("google_oauth_failed");
    }

    String fubUserEmail = String.valueOf(session.getAttribute(SESSION_USER_EMAIL));
    try {
      saveAuthorizedClient(fubUserEmail, exchangeCode(code));
      clearSession(session);
      return frontendRedirect("connected");
    } catch (RuntimeException error) {
      log.warn("Google Calendar authorization failed: {}", error.getClass().getSimpleName());
      clearSession(session);
      return frontendRedirect("google_oauth_failed");
    }
  }

  private TokenResponse exchangeCode(String code) {
    ClientRegistration registration = googleRegistration();
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", registration.getClientId());
    form.add("client_secret", registration.getClientSecret());
    form.add("redirect_uri", properties.getGoogleCalendar().getRedirectUri());
    form.add("grant_type", "authorization_code");

    Map<?, ?> response = restClient.post()
        .uri(registration.getProviderDetails().getTokenUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(Map.class);
    if (response == null || !(response.get("access_token") instanceof String accessToken)) {
      throw new IllegalStateException("Google token response did not contain an access token");
    }
    long expiresIn = response.get("expires_in") instanceof Number value ? value.longValue() : 3600L;
    String refreshToken = response.get("refresh_token") instanceof String value ? value : null;
    return new TokenResponse(accessToken, refreshToken, expiresIn);
  }

  private void saveAuthorizedClient(String fubUserEmail, TokenResponse token) {
    ClientRegistration registration = googleRegistration();
    Instant issuedAt = Instant.now();
    OAuth2AccessToken accessToken = new OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER,
        token.accessToken(),
        issuedAt,
        issuedAt.plusSeconds(token.expiresIn()),
        Set.of(CALENDAR_SCOPE)
    );
    OAuth2AuthorizedClient previous = authorizedClientService.loadAuthorizedClient("google", fubUserEmail);
    OAuth2RefreshToken refreshToken = token.refreshToken() == null && previous != null
        ? previous.getRefreshToken()
        : token.refreshToken() == null ? null : new OAuth2RefreshToken(token.refreshToken(), issuedAt);
    OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(
        registration,
        fubUserEmail,
        accessToken,
        refreshToken
    );
    Authentication principal = new UsernamePasswordAuthenticationToken(fubUserEmail, "N/A", List.of());
    authorizedClientService.saveAuthorizedClient(client, principal);
  }

  private boolean stateMatches(HttpSession session, String state) {
    Object expected = session.getAttribute(SESSION_STATE);
    Object email = session.getAttribute(SESSION_USER_EMAIL);
    return expected instanceof String expectedState
        && email instanceof String userEmail
        && !userEmail.isBlank()
        && expectedState.equals(state);
  }

  private void clearSession(HttpSession session) {
    if (session != null) {
      session.removeAttribute(SESSION_STATE);
      session.removeAttribute(SESSION_USER_EMAIL);
    }
  }

  private String frontendRedirect(String status) {
    return UriComponentsBuilder
        .fromUriString(properties.getFrontendBaseUrl())
        .path("/app/calendar")
        .queryParam("calendar", status)
        .build(true)
        .toUriString();
  }

  private ClientRegistration googleRegistration() {
    ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("google");
    if (registration == null) {
      throw new IllegalStateException("Google OAuth client is not configured");
    }
    return registration;
  }

  private record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
  }
}
