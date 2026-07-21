package com.foryour.delivery.client.agent;

import com.foryour.delivery.client.agent.AgentService.AgentSessionView;
import com.foryour.delivery.client.agent.AgentService.AgentMessageResult;
import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AgentController extends BaseController {

  private final AgentService agentService;

  @PostMapping("/wb/agent/sessions")
  public APIDataResponse<AgentSessionView> createSession(
      @Valid @RequestBody AgentSessionCreateRequest request
  ) {
    return APIDataResponse.of(agentService.createSession(
        getSessionInfo().getUserSq(), request.title(), request.context()));
  }

  @GetMapping("/wb/agent/sessions/{sessionSq}")
  public APIDataResponse<AgentSessionView> session(@PathVariable Long sessionSq) {
    return APIDataResponse.of(agentService.session(getSessionInfo().getUserSq(), sessionSq));
  }

  @PostMapping("/wb/agent/sessions/{sessionSq}/messages")
  public APIDataResponse<AgentMessageResult> sendMessage(
      @PathVariable Long sessionSq,
      @Valid @RequestBody AgentMessageRequest request
  ) {
    return APIDataResponse.of(agentService.sendMessage(
        getSessionInfo().getUserSq(), sessionSq, request.text(), request.context()));
  }

  public record AgentSessionCreateRequest(
      @Size(max = 200) String title,
      Map<String, Object> context
  ) {
  }

  public record AgentMessageRequest(
      @NotBlank @Size(max = 4000) String text,
      Map<String, Object> context
  ) {
  }
}
