package com.foryour.delivery.client.agent;

import com.foryour.delivery.common.CProperties;
import com.foryour.delivery.domain.enums.AgentTypeCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OpenAiAgentClient {

  private final CProperties.OpenAi properties;
  private final OpenAiAgentPrompts prompts;
  private final RestClient restClient;

  public OpenAiAgentClient(CProperties properties, OpenAiAgentPrompts prompts) {
    this.properties = properties.getOpenai();
    this.prompts = prompts;

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(this.properties.getConnectTimeoutMs()));
    requestFactory.setReadTimeout(Duration.ofMillis(this.properties.getReadTimeoutMs()));
    this.restClient = RestClient.builder()
        .baseUrl(this.properties.getBaseUrl())
        .requestFactory(requestFactory)
        .build();
  }

  public boolean isConfigured() {
    return properties.getApiKey() != null && !properties.getApiKey().isBlank();
  }

  public String configuredModel() {
    return properties.getModel();
  }

  public Optional<OpenAiReply> generate(AgentTypeCode agentType, Map<String, Object> safeContext) {
    return generate(prompts.instructions(agentType), safeContext, properties.getMaxOutputTokens());
  }

  public Optional<OpenAiReply> summarizeProduct(Map<String, Object> productContext) {
    return generate(prompts.productSummaryInstructions(), productContext,
        Math.min(properties.getMaxOutputTokens(), 220));
  }

  private Optional<OpenAiReply> generate(
      String instructions,
      Map<String, Object> safeContext,
      int maxOutputTokens
  ) {
    if (!isConfigured()) {
      return Optional.empty();
    }

    Map<String, Object> request = Map.of(
        "model", properties.getModel(),
        "instructions", instructions,
        "input", safeContext.toString(),
        "reasoning", Map.of("effort", properties.getReasoningEffort()),
        "max_output_tokens", maxOutputTokens,
        "store", false
    );

    try {
      Map<?, ?> response = restClient.post()
          .uri("/v1/responses")
          .header("Authorization", "Bearer " + properties.getApiKey())
          .body(request)
          .retrieve()
          .body(Map.class);
      String text = extractOutputText(response);
      if (text == null || text.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(new OpenAiReply(
          text.trim(),
          stringValue(response, "model", properties.getModel()),
          stringValue(response, "id", null)
      ));
    } catch (RuntimeException exception) {
      log.warn("OpenAI agent response unavailable: {}", exception.getClass().getSimpleName());
      return Optional.empty();
    }
  }

  private String extractOutputText(Map<?, ?> response) {
    if (response == null) {
      return null;
    }
    Object direct = response.get("output_text");
    if (direct instanceof String text && !text.isBlank()) {
      return text;
    }
    Object output = response.get("output");
    if (!(output instanceof List<?> items)) {
      return null;
    }
    for (Object item : items) {
      if (!(item instanceof Map<?, ?> outputItem)) {
        continue;
      }
      Object content = outputItem.get("content");
      if (!(content instanceof List<?> parts)) {
        continue;
      }
      for (Object part : parts) {
        if (part instanceof Map<?, ?> value && "output_text".equals(value.get("type"))
            && value.get("text") instanceof String text && !text.isBlank()) {
          return text;
        }
      }
    }
    return null;
  }

  private String stringValue(Map<?, ?> response, String key, String fallback) {
    if (response == null || response.get(key) == null) {
      return fallback;
    }
    return String.valueOf(response.get(key));
  }

  public record OpenAiReply(String text, String model, String responseId) {
  }
}
