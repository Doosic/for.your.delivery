package com.foryour.delivery.config.jwt;

import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.common.CProperties;
import com.foryour.delivery.domain.enums.UserStatusCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final CProperties cProperties;

  public void issueCookies(UserResponseVO user, HttpServletResponse response) {
    Map<String, Object> claims = Map.of(
        "userSq", user.getUserSq(),
        "email", user.getEmail(),
        "name", user.getName(),
        "status", user.getStatus().name()
    );
    Token token = generateTokenHS512(
        user.getEmail(),
        cProperties.getJwt().getAccessTimeoutMin(),
        cProperties.getJwt().getSecret(),
        claims
    );
    createAccessCookie(response, token.getAccessToken(), cProperties.getJwt().getAccessHeader(), cProperties.getJwt().getAccessTimeoutMin());
    createRefreshCookie(response, token.getRefreshToken(), cProperties.getJwt().getRefreshHeader(), cProperties.getJwt().getRefreshTimeoutMin());
  }

  public Token generateTokenHS512(
      String adminId,
      Integer timeOutMin,
      String secret,
      Map<String, Object> claims
  ){
    String accessToken = Jwts.builder()
        .setClaims(claims)
        .setId(adminId)
        .setExpiration(new Date(System.currentTimeMillis() + this.getTokenExpirationTime(timeOutMin)))
        .signWith(getSigningKey(secret), SignatureAlgorithm.HS512)
        .compact();

    String refreshToken = Jwts.builder()
        .setId(adminId)
        .setExpiration(new Date(System.currentTimeMillis() + this.getTokenExpirationTime(timeOutMin)))
        .signWith(getSigningKey(secret), SignatureAlgorithm.HS512)
        .compact();

    return Token.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  private Integer getTokenExpirationTime(Integer expirationTime) {
    return 1000 * 60 * expirationTime;
  }

  private SecretKey getSigningKey(String secret) {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public void createAccessCookie(
      HttpServletResponse response,
      String accessToken,
      String accessHeader,
      Integer timeOutMin
  ) {
    Cookie cookieAccess = new Cookie(accessHeader, accessToken);
    cookieAccess.setPath("/");
    cookieAccess.setMaxAge(60 * 60 * timeOutMin);
    cookieAccess.setHttpOnly(true);
    response.addCookie(cookieAccess);
  }

  public void createRefreshCookie(
      HttpServletResponse response,
      String refreshToken,
      String refreshHeader,
      Integer timeOutMin
  ) {
    Cookie cookieRefresh = new Cookie(refreshHeader, refreshToken);
    cookieRefresh.setPath("/");
    cookieRefresh.setMaxAge(60 * 60 * timeOutMin);
    cookieRefresh.setHttpOnly(true);
    response.addCookie(cookieRefresh);
  }

  public void resetAccessCookie(
      HttpServletResponse response,
      String accessHeader
  ){
    Cookie cookieAccess = new Cookie(accessHeader, null);
    cookieAccess.setPath("/");
    cookieAccess.setMaxAge(0);
    cookieAccess.setHttpOnly(true);
    response.addCookie(cookieAccess);
  }

  public void resetRefreshCookie(
      HttpServletResponse response,
      String refreshHeader
  ){
    Cookie cookieRefresh = new Cookie(refreshHeader, null);
    cookieRefresh.setPath("/");
    cookieRefresh.setMaxAge(0);
    cookieRefresh.setHttpOnly(true);
    response.addCookie(cookieRefresh);
  }

  public String getUsernameFromToken(String token) {
    return getClaimFromToken(token, Claims::getId);
  }

  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey(cProperties.getJwt().getSecret()))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(getSigningKey(cProperties.getJwt().getSecret()))
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (SignatureException e) {
      log.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }

  public Authentication getAuthentication(String accessToken) {
    Collection<SimpleGrantedAuthority> roles = new ArrayList<SimpleGrantedAuthority>();
    roles.add(new SimpleGrantedAuthority("ROLE_USER"));

    UserResponseVO adminInfo = parseTokenBody(accessToken);
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(adminInfo.getEmail(), adminInfo.getEmail(), roles);
    authenticationToken.setDetails(adminInfo);
    return authenticationToken;
  }

  public UserResponseVO parseTokenBody(String token){
    Claims claims = Jwts.parser()
        .verifyWith(getSigningKey(cProperties.getJwt().getSecret()))
        .build()
        .parseSignedClaims(token)
        .getPayload();

    return UserResponseVO.builder()
        .userSq(parseLongClaim(claims.get("userSq")))
        .email((String) claims.get("email"))
        .name((String) claims.get("name"))
        .status(UserStatusCode.valueOf((String) claims.get("status")))
        .build();
  }

  private Long parseLongClaim(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }

    return Long.parseLong(value.toString());
  }
}
