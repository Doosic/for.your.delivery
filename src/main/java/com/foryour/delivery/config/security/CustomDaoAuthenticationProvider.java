package com.foryour.delivery.config.security;


import com.foryour.delivery.client.user.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CustomDaoAuthenticationProvider extends DaoAuthenticationProvider {

  private UserService userService;

  public CustomDaoAuthenticationProvider(
      UserService userService,
      BCryptPasswordEncoder bCryptPasswordEncoder
  ) {
    super(userService);
    this.userService = userService;
    setPasswordEncoder(bCryptPasswordEncoder);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    try {
      Authentication auth = super.authenticate(authentication);

      if (auth.getPrincipal() instanceof UserDetails) {
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        userService.resetFailCount(userDetails.getUsername());
      }

      return auth;
    } catch (BadCredentialsException e) {
      if (authentication.getPrincipal() instanceof String) {
        String username = (String) authentication.getPrincipal();
        userService.increaseFailCount(username);
      }
      throw e;
    } catch (AuthenticationException e) {
      throw e;
    }
  }
}
