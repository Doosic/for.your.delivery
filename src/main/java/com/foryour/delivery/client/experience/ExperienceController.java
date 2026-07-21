package com.foryour.delivery.client.experience;

import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ExperienceController extends BaseController {

  @GetMapping("/wp/briefings/today")
  public APIDataResponse<Map<String, Object>> todayBriefing() {
    getSessionInfo();
    return APIDataResponse.of(Map.of(
        "summary", "재고와 연결된 일정을 기준으로 오늘 살 상품과 기다릴 상품을 나눴어요.",
        "sections", List.of(
            Map.of("title", "오늘 살 것", "items", List.of(
                decision("LOCAL-DETERGENT-1", "세탁세제 리필 2.6L", "재고 D-3 · 최근 가격 하락", "BUY_NOW", "BUY NOW"),
                decision("LOCAL-NAVER-2", "고양이 모래 6L x2", "예상 소진일 도달", "BUY_NOW", "BUY NOW")
            )),
            Map.of("title", "기다릴 것", "items", List.of(
                decision("LOCAL-NAVER-1", "고양이 사료 1.5kg", "재고 충분 · 다음 가격 확인 대기", "WAIT", "WAIT")
            )),
            Map.of("title", "일정 준비", "items", List.of(
                decision("LOCAL-CAMPING-GAS", "부탄가스 4개입", "캠핑 준비물 추천 · 구매 마감 D-2", "BUY_NOW", "BUY NOW"),
                decision("LOCAL-CAMPING-ICE", "아이스팩 대형", "캠핑 준비물 추천 · 재고 없음", "BUY_NOW", "BUY NOW"),
                decision("LOCAL-CAMPING-CHARCOAL", "화로용 숯 3kg", "캠핑 준비물 추천 · 가격 확인 대기", "WAIT", "WAIT")
            ))
        ),
        "traceId", "brief-demo-v1"
    ));
  }

  @GetMapping("/wp/agent/status")
  public APIDataResponse<Map<String, Object>> agentStatus() {
    getSessionInfo();
    return APIDataResponse.of(Map.of(
        "monitoring", List.of(
            Map.of("label", "재구매 모니터링", "value", "5개 품목 추적 중"),
            Map.of("label", "가격 추적", "value", "2건 · 하락 시 알림"),
            Map.of("label", "일정 준비", "value", "Google Calendar 연결 상태 반영")
        ),
        "decisions", List.of(
            decision("LOCAL-DETERGENT-1", "세탁세제 리필 2.6L", "재고 D-3 · 구매 권장", "BUY_NOW", "BUY NOW"),
            decision("LOCAL-NAVER-1", "고양이 사료 1.5kg", "다음 가격 확인까지 대기", "WAIT", "WAIT"),
            decision("LOCAL-WIPES-1", "물티슈 캡형 10팩", "재고 충분", "HOLD", "HOLD")
        ),
        "traceId", "agent-demo-v1"
    ));
  }

  @PostMapping("/wp/agent/chat/stream")
  public APIDataResponse<Map<String, Object>> chat(@RequestBody ChatRequest request) {
    getSessionInfo();
    String message = request.message() == null ? "" : request.message().trim();
    String topic = message.isBlank() ? "요청" : message;
    return APIDataResponse.of(Map.of(
        "role", "assistant",
        "text", topic + "에 필요한 후보를 재고, 일정, 가격 기준으로 정리했어요.",
        "card", Map.of(
            "title", "FUB 추천",
            "items", List.of(
                Map.of("name", "우선 준비 품목", "decision", "BUY_NOW", "label", "BUY NOW"),
                Map.of("name", "가격 확인 품목", "decision", "WAIT", "label", "WAIT")
            )
        ),
        "traceId", "chat-demo-v1"
    ));
  }

  private Map<String, String> decision(String productSq, String name, String note, String decision, String label) {
    return Map.of(
        "productSq", productSq,
        "name", name,
        "note", note,
        "decision", decision,
        "label", label
    );
  }

  public record ChatRequest(String message) {
  }
}
