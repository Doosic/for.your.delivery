package com.foryour.delivery.common;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalDataMaskerTest {

  private final PersonalDataMasker personalDataMasker = new PersonalDataMasker();

  @Test
  void masksPersonalAndCredentialValuesRecursively() {
    Map<String, Object> masked = personalDataMasker.mask(Map.of(
        "email", "lion4464@gmail.com",
        "phone", "010-1234-5678",
        "residentNumber", "990101-1234567",
        "nested", List.of("password: secret-value", "일반 정보")
    ));

    assertThat(masked.get("email")).isEqualTo("l***@gmail.com");
    assertThat(masked.get("phone")).isEqualTo("010-****-5678");
    assertThat(masked.get("residentNumber")).isEqualTo("******-*******");
    assertThat(masked.toString()).doesNotContain("secret-value", "990101-1234567", "010-1234-5678");
  }
}
