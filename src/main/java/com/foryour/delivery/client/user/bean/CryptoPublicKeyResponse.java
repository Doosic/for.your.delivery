package com.foryour.delivery.client.user.bean;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CryptoPublicKeyResponse {

  private String publicKeyModulus;
  private String publicKeyExponent;
}
