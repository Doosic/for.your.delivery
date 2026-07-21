package com.foryour.delivery.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {

  public static String getDateFormatString(LocalDateTime date) {
    if (date != null) {
      String formatDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      return formatDate;
    }
    return null;
  }
}
