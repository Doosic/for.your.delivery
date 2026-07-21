package com.foryour.delivery.client.user;

import com.foryour.delivery.client.user.bean.*;
import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.APIErrorResponse;
import com.foryour.delivery.common.BaseController;
import com.foryour.delivery.common.CProperties;
import com.foryour.delivery.config.jwt.JwtTokenProvider;
import com.foryour.delivery.config.jwt.Token;
import com.foryour.delivery.domain.entity.UserEntity;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.foryour.delivery.domain.enums.ErrorCode.LOGIN_FAIL;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController extends BaseController {

  private final UserService userService;
  private final RsaCryptoService rsaCryptoService;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final CProperties cProperties;

  @GetMapping("/wb/user/user-info")
  public APIDataResponse<UserInfoResponseVO> getUserInfo(){
    UserResponseVO userInfo = getSessionInfo();
    UserInfoResponseVO userInfoResponseVO = new UserInfoResponseVO();
    if(userInfo != null){
      userInfoResponseVO.setEmail(userInfo.getEmail());
      userInfoResponseVO.setName(userInfo.getName());
      userInfoResponseVO.setStatus(userInfo.getStatus());
    }
    return APIDataResponse.of(userInfoResponseVO);
  }

  @PostMapping("/wp/user/crypto-public-key")
  public APIDataResponse<CryptoPublicKeyResponse> cryptoPublicKey() {
    return APIDataResponse.of(rsaCryptoService.getPublicKey());
  }

  @PostMapping("/wp/user/signup")
  public APIDataResponse<UserResponseVO> signup(@Valid @RequestBody UserSignupRequest request) {
    return APIDataResponse.of(userService.signup(request));
  }

  @PostMapping("/wp/user/login")
  public Object login(@RequestBody UserLoginVO request, HttpServletResponse response) {
    String email = rsaCryptoService.decrypt(request.getEmail()).trim().toLowerCase();
    String password = rsaCryptoService.decrypt(request.getPassword());
    UserEntity user = userService.getActiveUserByEmail(email);

    if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
      return APIErrorResponse.of(false, LOGIN_FAIL.getCode(), LOGIN_FAIL.getMessage());
    }

    UserResponseVO userResponse = userService.getUserDetailsByEmail(email);
    Map<String, Object> claims = new HashMap<>();
    claims.put("userSq", userResponse.getUserSq());
    claims.put("email", userResponse.getEmail());
    claims.put("name", userResponse.getName());
    claims.put("status", userResponse.getStatus());

    Token jwtToken = jwtTokenProvider.generateTokenHS512(
        userResponse.getEmail(),
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

    return APIDataResponse.of(userResponse);
  }

  @GetMapping("/wb/user/me")
  public APIDataResponse<UserResponseVO> me() {
    return APIDataResponse.of(getSessionInfo());
  }

  @GetMapping("/wb/user/logout")
  public APIDataResponse<Void> logout(HttpServletResponse response) {
    jwtTokenProvider.resetAccessCookie(response, cProperties.getJwt().getAccessHeader());
    jwtTokenProvider.resetRefreshCookie(response, cProperties.getJwt().getRefreshHeader());
    return APIDataResponse.of(null);
  }
}
