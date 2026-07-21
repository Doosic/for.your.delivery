package com.foryour.delivery.client.event;

import com.foryour.delivery.common.APIDataResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@RestController
public class EventController {

  private static final int MAX_BUFFER_SIZE = 1_000;
  private final ConcurrentLinkedDeque<BehaviorEvent> eventBuffer = new ConcurrentLinkedDeque<>();

  @PostMapping("/wp/event")
  public ResponseEntity<APIDataResponse<Map<String, Object>>> collect(@Valid @RequestBody EventRequest request) {
    String eventId = UUID.randomUUID().toString();
    eventBuffer.addFirst(new BehaviorEvent(
        eventId,
        request.type(),
        request.targetType(),
        request.targetSq(),
        request.traceId(),
        Instant.now().toString()
    ));
    while (eventBuffer.size() > MAX_BUFFER_SIZE) eventBuffer.pollLast();

    return ResponseEntity.accepted().body(APIDataResponse.of(Map.of(
        "accepted", true,
        "eventId", eventId,
        "receivedAt", Instant.now().toString()
    )));
  }

  public record EventRequest(
      @NotBlank String type,
      @NotBlank String targetType,
      Long targetSq,
      String traceId
  ) {
  }

  private record BehaviorEvent(
      String eventId,
      String type,
      String targetType,
      Long targetSq,
      String traceId,
      String receivedAt
  ) {
  }
}
