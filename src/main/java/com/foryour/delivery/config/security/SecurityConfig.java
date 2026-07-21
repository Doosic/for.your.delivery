package com.foryour.delivery.config.security;

import com.foryour.delivery.client.user.UserService;
import com.foryour.delivery.common.CProperties;
import com.foryour.delivery.config.jwt.JwtAuthenticationEntryPoint;
import com.foryour.delivery.config.jwt.JwtRequestFilter;
import com.foryour.delivery.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final UserService userService;
  private final ObjectPostProcessor<Object> objectPostProcessor;
  private final CProperties cProperties;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtRequestFilter jwtRequestFilter;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;
  private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

  private static final String[] WHITE_LIST = {
      "/**"
  };

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      ClientRegistrationRepository clientRegistrationRepository
  ) throws Exception {
    http
        .httpBasic(withDefaults())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(WHITE_LIST).permitAll())
        .logout(AbstractHttpConfigurer::disable)
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(jwtAuthenticationEntryPoint))
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .formLogin(AbstractHttpConfigurer::disable)
        .oauth2Login(oauth -> oauth
            .authorizationEndpoint(endpoint -> endpoint
                .authorizationRequestResolver(googleAuthorizationRequestResolver(clientRegistrationRepository)))
            .successHandler(googleOAuth2SuccessHandler)
            .failureHandler((request, response, exception) -> response.sendRedirect(
                cProperties.getFrontendBaseUrl() + "/app/login?error=google_oauth_failed")))
        .addFilter(getAuthenticationFilter())
        .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowCredentials(true);
    config.setAllowedOrigins(Arrays.asList(
        "http://localhost:3500",
        "http://localhost:3501",
        "http://127.0.0.1:3500",
        "http://127.0.0.1:3501",
        "http://localhost:8087",
        "http://localhost:4173"
    ));
    config.setAllowedMethods(Arrays.asList("POST", "GET", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  private OAuth2AuthorizationRequestResolver googleAuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository
  ) {
    DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository,
        "/oauth2/authorization"
    );
    resolver.setAuthorizationRequestCustomizer(customizer -> customizer.additionalParameters(parameters -> {
      parameters.put("access_type", "offline");
      parameters.put("prompt", "consent");
    }));
    return resolver;
  }

  private AuthenticationFilter getAuthenticationFilter() throws Exception {
    AuthenticationManagerBuilder builder = new AuthenticationManagerBuilder(objectPostProcessor);
    AuthenticationFilter authenticationFilter = new AuthenticationFilter(
        this.authenticationManager(builder),
        userService,
        cProperties,
        jwtTokenProvider
    );
    return authenticationFilter;
  }


  public AuthenticationManager authenticationManager(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(this.customDaoAuthenticationProvider(bCryptPasswordEncoder));
    return auth.build();
  }

  @Bean
  public CustomDaoAuthenticationProvider customDaoAuthenticationProvider(BCryptPasswordEncoder bCryptPasswordEncoder) {
    CustomDaoAuthenticationProvider authenticationProvider = new CustomDaoAuthenticationProvider(userService, bCryptPasswordEncoder);
    return authenticationProvider;
  }

}
