package com.foryour.delivery.client.user;

import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.client.user.bean.UserSignupRequest;
import com.foryour.delivery.domain.entity.UserEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {

  UserResponseVO getUserDetailsByEmail(String email);

  UserResponseVO signup(UserSignupRequest request);

  UserEntity getActiveUserByEmail(String email);

  void logout(HttpServletResponse response);
  void increaseFailCount(String email);
  void resetFailCount(String email);
}
