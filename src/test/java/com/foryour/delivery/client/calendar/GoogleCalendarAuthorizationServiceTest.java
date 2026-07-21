package com.foryour.delivery.client.calendar;

import com.foryour.delivery.common.CProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleCalendarAuthorizationServiceTest {

  private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";

  private OAuth2AuthorizedClientService authorizedClientService;
  private GoogleCalendarAuthorizationService service;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    ClientRegistration registration = ClientRegistration.withRegistrationId("google")
        .clientId("test-client")
        .clientSecret("test-secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://localhost:30100/delivery/login/oauth2/code/google")
        .scope("openid", "profile", "email")
        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
        .tokenUri(TOKEN_URI)
        .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
        .userNameAttributeName("email")
        .clientName("Google")
        .build();
    CProperties properties = new CProperties();
    properties.setFrontendBaseUrl("http://localhost:3500");
    CProperties.GoogleCalendar calendar = new CProperties.GoogleCalendar();
    calendar.setRedirectUri("http://localhost:30100/delivery/wp/calendar/google/callback");
    properties.setGoogleCalendar(calendar);

    authorizedClientService = mock(OAuth2AuthorizedClientService.class);
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    service = new GoogleCalendarAuthorizationService(
        new InMemoryClientRegistrationRepository(registration),
        authorizedClientService,
        properties,
        builder.build()
    );
  }

  @Test
  void requestsOnlyCalendarReadScope() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    Map<String, String> query = query(service.authorizationUrl("member@example.com", request));

    assertThat(query.get("scope")).isEqualTo(GoogleCalendarAuthorizationService.CALENDAR_SCOPE);
    assertThat(query.get("scope")).doesNotContain("openid", "profile", "email");
    assertThat(query.get("redirect_uri"))
        .isEqualTo("http://localhost:30100/delivery/wp/calendar/google/callback");
    assertThat(query.get("access_type")).isEqualTo("offline");
    assertThat(query.get("state")).isNotBlank();
  }

  @Test
  void exchangesCodeAndStoresCalendarTokenForCurrentFubUser() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    String state = query(service.authorizationUrl("member@example.com", request)).get("state");
    server.expect(requestTo(TOKEN_URI))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(
            "{\"access_token\":\"calendar-access\",\"refresh_token\":\"calendar-refresh\",\"expires_in\":3600}",
            MediaType.APPLICATION_JSON
        ));

    String redirect = service.complete("authorization-code", state, null, request);

    assertThat(redirect).isEqualTo("http://localhost:3500/app/calendar?calendar=connected");
    var clientCaptor = org.mockito.ArgumentCaptor.forClass(OAuth2AuthorizedClient.class);
    verify(authorizedClientService).saveAuthorizedClient(clientCaptor.capture(), any(Authentication.class));
    OAuth2AuthorizedClient saved = clientCaptor.getValue();
    assertThat(saved.getPrincipalName()).isEqualTo("member@example.com");
    assertThat(saved.getAccessToken().getScopes())
        .containsExactly(GoogleCalendarAuthorizationService.CALENDAR_SCOPE);
    assertThat(saved.getRefreshToken().getTokenValue()).isEqualTo("calendar-refresh");
    server.verify();
  }

  private Map<String, String> query(String url) {
    String rawQuery = URI.create(url).getRawQuery();
    return Arrays.stream(rawQuery.split("&"))
        .map(value -> value.split("=", 2))
        .collect(Collectors.toMap(
            value -> decode(value[0]),
            value -> value.length == 1 ? "" : decode(value[1])
        ));
  }

  private String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
