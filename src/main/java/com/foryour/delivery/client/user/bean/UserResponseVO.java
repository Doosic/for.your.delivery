package com.foryour.delivery.client.user.bean;

import com.foryour.delivery.domain.enums.UserStatusCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseVO {

  private Long userSq;
  private String email;
  private String name;
  private UserStatusCode status;
}
