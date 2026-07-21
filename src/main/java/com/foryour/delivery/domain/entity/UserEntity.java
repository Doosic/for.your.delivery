package com.foryour.delivery.domain.entity;

import com.foryour.delivery.domain.enums.UserStatusCode;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="tb_fy_user")
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_sq")
  private Long userSq;

  @Column(name = "email")
  private String email;

  @Column(name = "name")
  private String name;

  @Column(name = "password")
  private String password;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private UserStatusCode status;
}
