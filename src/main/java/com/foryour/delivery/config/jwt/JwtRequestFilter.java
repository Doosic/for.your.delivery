package com.foryour.delivery.config.jwt;

import com.foryour.delivery.client.user.UserServiceImpl;
import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.common.APIErrorResponse;
import com.foryour.delivery.common.CProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static com.foryour.delivery.domain.enums.ErrorCode.UNAUTHORIZED_FAIL;


@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

  private final UserServiceImpl userServiceImpl;
  private final CProperties cProperties;
  private final JwtTokenProvider jwtTokenProvider;

  private AntPathMatcher pathMatcher = new AntPathMatcher();

  private Set<String> skipUrls = new HashSet<>(Arrays.asList(
      "/login",
      "/logout",
      "/login/**",
      "/wp/**"
  ));

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    return skipUrls.stream().anyMatch(p -> pathMatcher.match(p, request.getServletPath()));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

    if(request.getCookies() == null){
      this.unathorizedFail(response);
      return;
    }

    String accessToken = Arrays.stream(request.getCookies())
        .filter(cookie -> cookie.getName().equals(cProperties.getJwt().getAccessHeader()))
        .findFirst().map(Cookie::getValue)
        .orElse(null);

    String refreshToken = Arrays.stream(request.getCookies())
        .filter(cookie -> cookie.getName().equals(cProperties.getJwt().getRefreshHeader()))
        .findFirst().map(Cookie::getValue)
        .orElse(null);

    String userEmail = null;

    if(accessToken != null && refreshToken != null && jwtTokenProvider.validateToken(accessToken)){
      if(jwtTokenProvider.validateToken(accessToken)){
        userEmail = jwtTokenProvider.getUsernameFromToken(accessToken);
      }else if(jwtTokenProvider.validateToken(refreshToken)){
        userEmail = jwtTokenProvider.getUsernameFromToken(refreshToken);

        UserResponseVO user = userServiceImpl.getUserDetailsByEmail(userEmail);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userSq",user.getUserSq());
        claims.put("email",user.getEmail());
        claims.put("name",user.getName());
        claims.put("status",user.getStatus());


        Token jwtToken = jwtTokenProvider.generateTokenHS512(
            userEmail,
            cProperties.getJwt().getAccessTimeoutMin(),
            cProperties.getJwt().getSecret(),
            claims);

        jwtTokenProvider.createAccessCookie(
            response,
            jwtToken.getAccessToken(),
            cProperties.getJwt().getAccessHeader(),
            cProperties.getJwt().getAccessTimeoutMin());

        jwtTokenProvider.createRefreshCookie(
            response,
            jwtToken.getRefreshToken(),
            cProperties.getJwt().getRefreshHeader(),
            cProperties.getJwt().getAccessTimeoutMin());
      }
    }

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null){
      Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }


  private void unathorizedFail(HttpServletResponse response){
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    try (PrintWriter writer = response.getWriter()) {
      writer.write(new ObjectMapper().writeValueAsString(APIErrorResponse.of(false, UNAUTHORIZED_FAIL.getCode(), UNAUTHORIZED_FAIL.getMessage())));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
