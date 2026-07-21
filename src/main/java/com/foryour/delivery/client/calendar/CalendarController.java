package com.foryour.delivery.client.calendar;

import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import com.foryour.delivery.client.user.bean.UserResponseVO;

@RestController
@RequiredArgsConstructor
public class CalendarController extends BaseController {

  private final GoogleCalendarService googleCalendarService;

  @GetMapping("/wb/calendar/purchase-suggestions")
  public APIDataResponse<GoogleCalendarService.CalendarResult> suggestions(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
  ) {
    LocalDate start = from == null ? LocalDate.now() : from;
    LocalDate end = to == null ? start.plusDays(30) : to;
    UserResponseVO user = getSessionInfo();
    return APIDataResponse.of(googleCalendarService.storedSuggestions(
        user.getUserSq(), start, end));
  }
}
