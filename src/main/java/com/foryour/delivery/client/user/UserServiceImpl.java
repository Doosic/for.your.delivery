package com.foryour.delivery.client.user;

import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.client.user.bean.UserSignupRequest;
import com.foryour.delivery.domain.entity.UserEntity;
import com.foryour.delivery.domain.enums.UserStatusCode;
import com.foryour.delivery.domain.repository.UserRepository;
import com.foryour.delivery.exception.AccountException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static com.foryour.delivery.domain.enums.ErrorCode.DATA_NOT_EXIST;
import static com.foryour.delivery.domain.enums.ErrorCode.DUPLICATED_DATA;
import static com.foryour.delivery.domain.enums.ErrorCode.LOCKED_ACCOUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  @Override
  @Transactional(readOnly = true)
  public UserResponseVO getUserDetailsByEmail(String email){
    UserEntity user = getActiveUserByEmail(email);

    return toResponse(user);
  }

  @Override
  @Transactional
  public UserResponseVO signup(UserSignupRequest request) {
    String email = request.getEmail().trim().toLowerCase();

    if (userRepository.existsByEmail(email)) {
      throw new AccountException(DUPLICATED_DATA);
    }

    UserEntity user = new UserEntity();
    user.setEmail(email);
    user.setName(request.getName().trim());
    user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
    user.setStatus(UserStatusCode.USE);

    return toResponse(userRepository.save(user));
  }

  @Override
  @Transactional
  public UserResponseVO findOrCreateGoogleUser(String email, String name) {
    String normalizedEmail = email.trim().toLowerCase();
    Optional<UserEntity> userOptional = userRepository.findByEmail(normalizedEmail);
    UserEntity user;
    if (userOptional.isPresent()) {
      user = userOptional.get();
    } else {
      UserEntity created = new UserEntity();
      created.setEmail(normalizedEmail);
      created.setName(name == null || name.isBlank() ? normalizedEmail : name.trim());
      created.setPassword(bCryptPasswordEncoder.encode(UUID.randomUUID().toString()));
      created.setStatus(UserStatusCode.USE);
      user = userRepository.save(created);
    }

    if (!UserStatusCode.USE.equals(user.getStatus())) {
      throw new AccountException(LOCKED_ACCOUNT);
    }
    return toResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserEntity getActiveUserByEmail(String email) {
    Optional<UserEntity> userOptional = userRepository.findByEmail(email.trim().toLowerCase());
    if (userOptional.isEmpty()) {
      throw new AccountException(DATA_NOT_EXIST);
    }
    UserEntity user = userOptional.get();

    if (UserStatusCode.LOCK.equals(user.getStatus()) || UserStatusCode.DELETE.equals(user.getStatus())) {
      throw new AccountException(LOCKED_ACCOUNT);
    }

    return user;
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
    Optional<UserEntity> userOptional = userRepository.findByEmail(username.trim().toLowerCase());
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException("user not found");
    }
    UserEntity user = userOptional.get();

    boolean enabled = UserStatusCode.USE.equals(user.getStatus());

    return User.builder()
        .username(user.getEmail())
        .password(user.getPassword())
        .roles("USER")
        .disabled(!enabled)
        .accountLocked(UserStatusCode.LOCK.equals(user.getStatus()))
        .build();
  }

  private UserResponseVO toResponse(UserEntity user) {
    return UserResponseVO.builder()
        .userSq(user.getUserSq())
        .email(user.getEmail())
        .name(user.getName())
        .status(user.getStatus())
        .build();
  }
}
