package com.foryour.delivery.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "delivery")
@Data
public class CProperties {

  private Jwt jwt;
  private String frontendBaseUrl;
  private External external = new External();

  @Data
  public static class Jwt {
    private String accessHeader;
    private String refreshHeader;
    private Integer accessTimeoutMin;
    private Integer refreshTimeoutMin;
    private String secret;
  }

  @Data
  public static class External {
    private Naver naver = new Naver();
    private Elevenst elevenst = new Elevenst();
  }

  @Data
  public static class Naver {
    private String clientId;
    private String clientSecret;
  }

  @Data
  public static class Elevenst {
    private String apiKey;
  }
}
