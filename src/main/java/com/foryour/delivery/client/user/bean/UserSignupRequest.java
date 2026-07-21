package com.foryour.delivery.client.user.bean;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSignupRequest {

  @NotBlank(message = "이름을 입력해 주세요.")
  private String name;

  @Email(message = "올바른 이메일 형식이 아닙니다.")
  @NotBlank(message = "이메일을 입력해 주세요.")
  private String email;

  @Size(min = 8, message = "비밀번호는 8자 이상 입력해 주세요.")
  @NotBlank(message = "비밀번호를 입력해 주세요.")
  private String password;
}
