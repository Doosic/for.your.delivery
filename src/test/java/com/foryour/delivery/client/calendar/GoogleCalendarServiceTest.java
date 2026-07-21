package com.foryour.delivery.client.calendar;

import com.foryour.delivery.common.PersonalDataMasker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleCalendarServiceTest {

  private final GoogleCalendarService service = new GoogleCalendarService(
      null, null, null, new PersonalDataMasker());

  @Test
  void extractsPreparationKeywordsByCalendarContext() {
    var camping = service.preparationTemplates("주말 캠핑", "가족 야영", "가평");
    var hospital = service.preparationTemplates("반려묘 병원", "정기 검진", "동물병원");
    var ordinary = service.preparationTemplates("팀 주간회의", "업무 공유", "회의실");

    assertThat(camping).extracting(GoogleCalendarService.PreparationTemplate::keyword)
        .containsExactly("모기퇴치제", "캠핑 아이스박스", "캠핑 숯");
    assertThat(hospital).extracting(GoogleCalendarService.PreparationTemplate::keyword)
        .containsExactly("반려동물 이동장 패드", "반려동물 간식");
    assertThat(ordinary).isEmpty();
  }
}
