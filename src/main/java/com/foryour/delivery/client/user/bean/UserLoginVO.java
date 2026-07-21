package com.foryour.delivery.client.user.bean;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserLoginVO {

  @NotBlank(message = "Email cannot be null")
  @Size(min = 2, message = "Email not be less than two characters")
  private String email;

  @NotBlank(message = "Password cannot be null")
  @Size(min = 8, message = "Password must be equals or grater than 8 characters")
  private String password;
}
