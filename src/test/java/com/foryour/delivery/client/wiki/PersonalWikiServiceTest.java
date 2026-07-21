package com.foryour.delivery.client.wiki;

import com.foryour.delivery.client.wiki.PersonalWikiService.WikiAgentReply;
import com.foryour.delivery.client.wiki.PersonalWikiService.WikiEntryView;
import com.foryour.delivery.client.wiki.PersonalWikiService.WikiView;
import com.foryour.delivery.domain.entity.UserEntity;
import com.foryour.delivery.domain.enums.UserStatusCode;
import com.foryour.delivery.domain.enums.WikiEntryStatusCode;
import com.foryour.delivery.domain.repository.UserRepository;
import com.foryour.delivery.domain.repository.WikiEntryHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PersonalWikiServiceTest {

  @Autowired
  private PersonalWikiService personalWikiService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WikiEntryHistoryRepository wikiEntryHistoryRepository;

  @Test
  void createsConfirmedWikiFromOnboardingAndMasksPrompt() {
    UserEntity user = createUser();
    long historyCount = wikiEntryHistoryRepository.count();

    WikiView result = personalWikiService.createOnboarding(
        user.getUserSq(),
        2,
        List.of("CAT"),
        List.of("LOWEST_PRICE", "FAST_DELIVERY"),
        List.of("PET_FOOD"),
        List.of("한식"),
        "주 3~4회",
        List.of("캠핑"),
        "연락 이메일은 lion4464@gmail.com 이고 무료배송을 선호해"
    );

    assertThat(result.entries()).hasSize(8);
    assertThat(result.entries()).allMatch(entry -> entry.status().equals("ACTIVE"));
    WikiEntryView prompt = result.entries().stream()
        .filter(entry -> entry.entryKey().equals("ONBOARDING_PROMPT"))
        .findFirst()
        .orElseThrow();
    assertThat(prompt.summary()).contains("l***@gmail.com").doesNotContain("lion4464@gmail.com");
    assertThat(wikiEntryHistoryRepository.count()).isEqualTo(historyCount + 8);
  }

  @Test
  void proposesChatFactAndUsesItOnlyAfterConfirmation() {
    UserEntity user = createUser();
    long historyCount = wikiEntryHistoryRepository.count();

    WikiAgentReply reply = personalWikiService.respondToChat(
        user.getUserSq(), 100L, "고양이 한 마리를 키우는 걸 기억해줘");
    WikiView pending = personalWikiService.wiki(
        user.getUserSq(), WikiEntryStatusCode.PENDING_CONFIRMATION, null, null);

    assertThat(reply.candidateWikiEntrySq()).isNotNull();
    assertThat(pending.entries()).hasSize(1);
    assertThat(personalWikiService.wiki(
        user.getUserSq(), WikiEntryStatusCode.ACTIVE, null, null).entries()).isEmpty();

    WikiEntryView confirmed = personalWikiService.confirm(
        user.getUserSq(), reply.candidateWikiEntrySq(), true);

    assertThat(confirmed.status()).isEqualTo("ACTIVE");
    assertThat(confirmed.version()).isEqualTo(2);
    assertThat(personalWikiService.wiki(
        user.getUserSq(), WikiEntryStatusCode.ACTIVE, "PET", "고양이").entries()).hasSize(1);
    assertThat(wikiEntryHistoryRepository.count()).isEqualTo(historyCount + 2);
  }

  private UserEntity createUser() {
    UserEntity user = new UserEntity();
    user.setEmail("wiki-service-test-" + UUID.randomUUID() + "@example.com");
    user.setName("wiki service test");
    user.setPassword("test-password");
    user.setStatus(UserStatusCode.USE);
    return userRepository.saveAndFlush(user);
  }
}
