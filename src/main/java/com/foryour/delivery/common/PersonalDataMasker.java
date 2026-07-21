package com.foryour.delivery.common;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Component
public class PersonalDataMasker {

  private static final Pattern PASSWORD = Pattern.compile(
      "(?i)(비밀번호|패스워드|password|인증번호)\\s*[:=]?\\s*([^\\s,]+)"
  );
  private static final Pattern EMAIL = Pattern.compile(
      "([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})"
  );
  private static final Pattern PHONE = Pattern.compile(
      "(?<!\\d)(01[016789])[-. ]?(\\d{3,4})[-. ]?(\\d{4})(?!\\d)"
  );
  private static final Pattern RESIDENT_NUMBER = Pattern.compile(
      "(?<!\\d)\\d{6}[- ]?[1-8]\\d{6}(?!\\d)"
  );
  private static final Pattern FINANCIAL_NUMBER = Pattern.compile(
      "(?<!\\d)(?:\\d[- ]?){12,18}\\d(?!\\d)"
  );
  private static final Pattern ACCOUNT_NUMBER = Pattern.compile(
      "(?i)(계좌(?:번호)?|account)\\s*[:=]?\\s*([0-9-]{8,30})"
  );

  public String mask(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }

    String masked = PASSWORD.matcher(value).replaceAll("$1: [MASKED]");
    masked = EMAIL.matcher(masked).replaceAll(this::maskEmail);
    masked = PHONE.matcher(masked).replaceAll("$1-****-$3");
    masked = RESIDENT_NUMBER.matcher(masked).replaceAll("******-*******");
    masked = ACCOUNT_NUMBER.matcher(masked).replaceAll("$1: [MASKED]");
    return FINANCIAL_NUMBER.matcher(masked).replaceAll("[NUMBER_MASKED]");
  }

  public Map<String, Object> mask(Map<String, Object> values) {
    if (values == null || values.isEmpty()) {
      return new LinkedHashMap<>();
    }
    Map<String, Object> masked = new LinkedHashMap<>();
    values.forEach((key, value) -> masked.put(key, maskValue(value)));
    return masked;
  }

  private Object maskValue(Object value) {
    if (value instanceof String text) {
      return mask(text);
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> nested = new LinkedHashMap<>();
      map.forEach((key, nestedValue) -> nested.put(String.valueOf(key), maskValue(nestedValue)));
      return nested;
    }
    if (value instanceof List<?> list) {
      List<Object> masked = new ArrayList<>();
      list.forEach(item -> masked.add(maskValue(item)));
      return masked;
    }
    return value;
  }

  private String maskEmail(MatchResult result) {
    String local = result.group(1);
    String visible = local.substring(0, 1);
    return visible + "***@" + result.group(2);
  }
}
