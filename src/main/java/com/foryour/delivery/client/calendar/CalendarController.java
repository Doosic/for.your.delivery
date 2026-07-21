package com.foryour.delivery.client.calendar;

import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CalendarController extends BaseController {

  private final GoogleCalendarService googleCalendarService;

  @GetMapping("/wp/calendar/purchase-suggestions")
  public APIDataResponse<GoogleCalendarService.CalendarResult> suggestions(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
  ) {
    LocalDate start = from == null ? LocalDate.now() : from;
    LocalDate end = to == null ? start.plusDays(30) : to;
    return APIDataResponse.of(googleCalendarService.suggestions(
        getSessionInfo().getEmail(), start, end, List.of("primary")
    ));
  }

  @PostMapping("/wp/calendar/sync")
  public APIDataResponse<Map<String, Object>> sync(@RequestBody SyncRequest request) {
    List<String> calendarIds = request.calendarIds() == null || request.calendarIds().isEmpty()
        ? List.of("primary")
        : request.calendarIds();
    LocalDate start = request.from() == null ? LocalDate.now() : request.from();
    LocalDate end = request.to() == null ? start.plusDays(30) : request.to();
    GoogleCalendarService.CalendarResult result = googleCalendarService.suggestions(
        getSessionInfo().getEmail(), start, end, calendarIds
    );
    return APIDataResponse.of(Map.of(
        "connected", result.connected(),
        "live", result.live(),
        "eventCount", result.suggestions().size(),
        "suggestionCount", result.suggestions().size()
    ));
  }

  public record SyncRequest(
      List<String> calendarIds,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
  ) {
  }
}
