package com.foryour.delivery.client.user;

import com.foryour.delivery.client.user.bean.CryptoPublicKeyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.HexFormat;

@Slf4j
@Service
public class RsaCryptoService {

  private final KeyPair keyPair;

  public RsaCryptoService() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(1024);
      this.keyPair = generator.generateKeyPair();
    } catch (Exception e) {
      throw new IllegalStateException("RSA key generation failed", e);
    }
  }

  public CryptoPublicKeyResponse getPublicKey() {
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

    return CryptoPublicKeyResponse.builder()
        .publicKeyModulus(toHex(publicKey.getModulus()))
        .publicKeyExponent(toHex(publicKey.getPublicExponent()))
        .build();
  }

  public String decrypt(String encryptedHex) {
    try {
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
      byte[] decrypted = cipher.doFinal(HexFormat.of().parseHex(encryptedHex));
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.warn("RSA decrypt failed", e);
      throw new IllegalArgumentException("암호화된 로그인 정보를 확인할 수 없습니다.");
    }
  }

  private String toHex(BigInteger value) {
    return value.toString(16);
  }
}
