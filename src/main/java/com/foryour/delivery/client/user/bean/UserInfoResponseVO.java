package com.foryour.delivery.client.user.bean;

import com.foryour.delivery.domain.enums.UserStatusCode;
import lombok.Data;

@Data
public class UserInfoResponseVO {
  private Long userSq;
  private String email;
  private String name;
  private UserStatusCode status;
}
