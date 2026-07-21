package com.foryour.delivery.config.security;

import com.foryour.delivery.client.user.UserService;
import com.foryour.delivery.client.user.UserServiceImpl;
import com.foryour.delivery.client.user.bean.UserLoginVO;
import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.APIErrorResponse;
import com.foryour.delivery.common.CProperties;
import com.foryour.delivery.config.jwt.JwtTokenProvider;
import com.foryour.delivery.config.jwt.Token;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.foryour.delivery.domain.enums.ErrorCode.LOCKED_ACCOUNT;
import static com.foryour.delivery.domain.enums.ErrorCode.LOGIN_FAIL;

public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter implements AuthenticationFailureHandler {

  private UserService userService;
  private CProperties cProperties;

  private JwtTokenProvider jwtTokenProvider;

  public AuthenticationFilter(AuthenticationManager authenticationManager,
                              UserService userService,
                              CProperties cProperties,
                              JwtTokenProvider jwtTokenProvider) {
    super.setAuthenticationManager(authenticationManager);
    super.setAuthenticationFailureHandler(this::onAuthenticationFailure);
    this.userService = userService;
    this.cProperties = cProperties;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
                                              HttpServletResponse response) throws AuthenticationException {
    try{
      UserLoginVO creds = new ObjectMapper().readValue(request.getInputStream(), UserLoginVO.class);

      return getAuthenticationManager().authenticate(
          new UsernamePasswordAuthenticationToken(
              creds.getEmail(),
              creds.getPassword(),
              new ArrayList<>()
          )
      );
    }catch(IOException e){
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication authResult) throws IOException, ServletException {
    String userName = ((User)authResult.getPrincipal()).getUsername();
    UserResponseVO user = userService.getUserDetailsByEmail(userName);

    Map<String, Object> claims = new HashMap<>();
    claims.put("userSq",user.getUserSq());
    claims.put("email",user.getEmail());
    claims.put("name",user.getName());
    claims.put("status",user.getStatus());

    Token jwtToken = jwtTokenProvider.generateTokenHS512(
        user.getEmail(),
        cProperties.getJwt().getAccessTimeoutMin(),
        cProperties.getJwt().getRefreshTimeoutMin(),
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
        cProperties.getJwt().getRefreshTimeoutMin());

    UserResponseVO loginResponseDto = UserResponseVO.builder()
        .userSq(user.getUserSq())
        .email(user.getEmail())
        .name(user.getName())
        .status(user.getStatus())
        .build();

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    try(PrintWriter writer = response.getWriter()){
      writer.write(new ObjectMapper().writeValueAsString(APIDataResponse.of(loginResponseDto)));
    }catch (IOException e){
      e.printStackTrace();
    }
  }



  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
    jwtTokenProvider.resetAccessCookie(response, cProperties.getJwt().getAccessHeader());
    jwtTokenProvider.resetRefreshCookie(response,  cProperties.getJwt().getRefreshHeader());

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    try (PrintWriter writer = response.getWriter()) {
      int errorCode = LOGIN_FAIL.getCode();
      String errorMessage = LOGIN_FAIL.getMessage();

      if(exception.getMessage().equals(LOCKED_ACCOUNT.getMessage())){
        errorCode = LOCKED_ACCOUNT.getCode();
        errorMessage = LOCKED_ACCOUNT.getMessage();
      }
      writer.write(new ObjectMapper().writeValueAsString(APIErrorResponse.of(false, errorCode, errorMessage)));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
