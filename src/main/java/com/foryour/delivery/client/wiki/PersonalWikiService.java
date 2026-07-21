package com.foryour.delivery.client.wiki;

import com.foryour.delivery.common.PersonalDataMasker;
import com.foryour.delivery.domain.entity.WikiEntryEntity;
import com.foryour.delivery.domain.entity.WikiEntryHistoryEntity;
import com.foryour.delivery.domain.enums.AgentMessageTypeCode;
import com.foryour.delivery.domain.enums.WikiEntryStatusCode;
import com.foryour.delivery.domain.enums.WikiSensitivityCode;
import com.foryour.delivery.domain.repository.WikiEntryHistoryRepository;
import com.foryour.delivery.domain.repository.WikiEntryRepository;
import com.foryour.delivery.exception.APIException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.foryour.delivery.domain.enums.ErrorCode.BAD_REQUEST;
import static com.foryour.delivery.domain.enums.ErrorCode.DATA_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class PersonalWikiService {

  private static final List<String> RESTRICTED_KEYWORDS = List.of(
      "비밀번호", "인증번호", "password"
  );

  private final WikiEntryRepository wikiEntryRepository;
  private final WikiEntryHistoryRepository wikiEntryHistoryRepository;
  private final PersonalDataMasker personalDataMasker;

  @Transactional(readOnly = true)
  public WikiView wiki(Long userSq, WikiEntryStatusCode status, String category, String query) {
    String normalizedCategory = normalizeOptional(category);
    String normalizedQuery = normalizeOptional(query);
    List<WikiEntryEntity> foundEntries =
        wikiEntryRepository.findAllByUserSqAndStatusOrderByModifiedDateDesc(userSq, status);
    List<WikiEntryView> entries = new ArrayList<>();
    for (WikiEntryEntity entry : foundEntries) {
      if (normalizedCategory != null && !entry.getCategory().equals(normalizedCategory)) {
        continue;
      }
      if (!matches(entry, normalizedQuery)) {
        continue;
      }
      entries.add(toView(entry));
    }

    return new WikiView(entries, entries.size(), status.name());
  }

  @Transactional
  public WikiEntryView createExplicit(
      Long userSq,
      String category,
      String entryKey,
      String summary,
      Map<String, Object> content
  ) {
    rejectRestricted(summary);
    String maskedSummary = personalDataMasker.mask(summary.trim());
    String normalizedCategory = normalizeRequired(category);
    String normalizedKey = normalizeKey(entryKey);
    Optional<WikiEntryEntity> entryOptional =
        wikiEntryRepository.findByUserSqAndCategoryAndEntryKey(userSq, normalizedCategory, normalizedKey);
    WikiEntryEntity entry;
    if (entryOptional.isPresent()) {
      entry = entryOptional.get();
    } else {
      entry = new WikiEntryEntity();
    }

    boolean created = entry.getWikiEntrySq() == null;
    if (created) {
      entry.setUserSq(userSq);
      entry.setCategory(normalizedCategory);
      entry.setEntryKey(normalizedKey);
      entry.setVersion(1);
    } else {
      entry.setVersion(entry.getVersion() + 1);
    }
    entry.setSummary(maskedSummary);
    entry.setContentJson(personalDataMasker.mask(content == null ? Map.of() : content));
    entry.setSourceType("USER");
    entry.setSourceRefSq(null);
    entry.setConfidence(BigDecimal.ONE);
    entry.setStatus(WikiEntryStatusCode.ACTIVE);
    entry.setSensitivityLevel(WikiSensitivityCode.NORMAL);
    entry.setValidUntil(null);
    entry = wikiEntryRepository.save(entry);
    saveHistory(entry, "USER", created ? "EXPLICIT_CREATE" : "EXPLICIT_UPDATE");

    return toView(entry);
  }

  @Transactional
  public WikiEntryView updateExplicit(
      Long userSq,
      Long wikiEntrySq,
      String category,
      String summary,
      Map<String, Object> content
  ) {
    WikiEntryEntity entry = ownedEntry(userSq, wikiEntrySq);
    rejectRestricted(summary);
    entry.setCategory(normalizeRequired(category));
    entry.setSummary(personalDataMasker.mask(summary.trim()));
    entry.setContentJson(personalDataMasker.mask(content == null ? Map.of() : content));
    entry.setSourceType("USER");
    entry.setSourceRefSq(null);
    entry.setConfidence(BigDecimal.ONE);
    entry.setStatus(WikiEntryStatusCode.ACTIVE);
    entry.setVersion(entry.getVersion() + 1);
    entry = wikiEntryRepository.save(entry);
    saveHistory(entry, "USER", "EXPLICIT_UPDATE");
    return toView(entry);
  }

  @Transactional
  public WikiEntryView archive(Long userSq, Long wikiEntrySq) {
    WikiEntryEntity entry = ownedEntry(userSq, wikiEntrySq);
    entry.setStatus(WikiEntryStatusCode.ARCHIVED);
    entry.setVersion(entry.getVersion() + 1);
    entry = wikiEntryRepository.save(entry);
    saveHistory(entry, "USER", "ARCHIVED");
    return toView(entry);
  }

  @Transactional
  public WikiView createOnboarding(
      Long userSq,
      Integer householdSize,
      List<String> pets,
      List<String> shoppingPriorities,
      List<String> preferredCategories,
      List<String> favoriteFoods,
      String cookingFrequency,
      List<String> hobbies,
      String prompt
  ) {
    if (householdSize != null) {
      createExplicit(userSq, "PROFILE", "HOUSEHOLD_SIZE", "가구원 " + householdSize + "명",
          Map.of("householdSize", householdSize));
    }
    if (pets != null && !pets.isEmpty()) {
      createExplicit(userSq, "PET", "PETS", "반려동물 정보", Map.of("pets", pets));
    }
    if (shoppingPriorities != null && !shoppingPriorities.isEmpty()) {
      createExplicit(userSq, "SHOPPING", "SHOPPING_PRIORITIES", "쇼핑 우선순위",
          Map.of("priorities", shoppingPriorities));
    }
    if (preferredCategories != null && !preferredCategories.isEmpty()) {
      createExplicit(userSq, "SHOPPING", "PREFERRED_CATEGORIES", "선호 상품 카테고리",
          Map.of("categories", preferredCategories));
    }
    if (favoriteFoods != null && !favoriteFoods.isEmpty()) {
      createExplicit(userSq, "FOOD", "FAVORITE_FOODS", "좋아하는 음식",
          Map.of("foods", favoriteFoods));
    }
    if (cookingFrequency != null && !cookingFrequency.isBlank()) {
      String frequency = cookingFrequency.trim();
      createExplicit(userSq, "LIFESTYLE", "COOKING_FREQUENCY", "직접 요리 빈도: " + frequency,
          Map.of("frequency", frequency));
    }
    if (hobbies != null && !hobbies.isEmpty()) {
      createExplicit(userSq, "HOBBY", "HOBBIES", "취미 정보",
          Map.of("hobbies", hobbies));
    }
    if (prompt != null && !prompt.isBlank()) {
      createExplicit(userSq, "PREFERENCE", "ONBOARDING_PROMPT", prompt, Map.of("prompt", prompt));
    }
    return wiki(userSq, WikiEntryStatusCode.ACTIVE, null, null);
  }

  @Transactional
  public WikiAgentReply respondToChat(Long userSq, Long messageSq, String text) {
    if (isRememberRequest(text)) {
      String summary = extractRememberedText(text);
      if (summary.isBlank()) {
        return new WikiAgentReply(
            AgentMessageTypeCode.TEXT,
            "기억할 내용을 함께 알려주세요.",
            Map.of("entries", List.of()),
            List.of(),
            null
        );
      }
      if (containsRestricted(summary)) {
        return new WikiAgentReply(
            AgentMessageTypeCode.TEXT,
            "비밀번호, 인증번호, 금융 식별정보는 개인 위키에 저장할 수 없어요.",
            Map.of("entries", List.of()),
            List.of(),
            null
        );
      }

      WikiEntryEntity candidate = createCandidate(userSq, messageSq, summary);
      WikiEntryView view = toView(candidate);
      return new WikiAgentReply(
          AgentMessageTypeCode.ACTION_CONFIRMATION,
          "이 내용을 개인 위키에 기억할까요? 확인 후에만 다른 Agent가 사용합니다.",
          Map.of("candidate", view),
          List.of(
              Map.of("type", "CONFIRM_WIKI_ENTRY", "wikiEntrySq", candidate.getWikiEntrySq()),
              Map.of("type", "REJECT_WIKI_ENTRY", "wikiEntrySq", candidate.getWikiEntrySq())
          ),
          candidate.getWikiEntrySq()
      );
    }

    WikiView result = wiki(userSq, WikiEntryStatusCode.ACTIVE, null, extractQuery(text));
    String response = result.entries().isEmpty()
        ? "확인된 개인 위키에서 관련 정보를 찾지 못했어요."
        : "확인된 개인 위키에서 관련 정보 " + result.totalCount() + "건을 찾았어요.";
    return new WikiAgentReply(
        AgentMessageTypeCode.TEXT,
        response,
        Map.of("entries", result.entries()),
        List.of(),
        null
    );
  }

  @Transactional
  public WikiEntryView proposeImportantFact(Long userSq, Long messageSq, String text) {
    String maskedText = personalDataMasker.mask(text);
    if (!looksImportant(maskedText) || containsRestricted(maskedText)) {
      return null;
    }
    return toView(createCandidate(userSq, messageSq, maskedText));
  }

  @Transactional
  public WikiEntryView confirm(Long userSq, Long wikiEntrySq, boolean approved) {
    Optional<WikiEntryEntity> entryOptional = wikiEntryRepository.findByWikiEntrySqAndUserSq(wikiEntrySq, userSq);
    if (entryOptional.isEmpty()) {
      throw new APIException(DATA_NOT_EXIST);
    }
    WikiEntryEntity entry = entryOptional.get();
    if (!WikiEntryStatusCode.PENDING_CONFIRMATION.equals(entry.getStatus())) {
      throw new APIException(BAD_REQUEST);
    }

    entry.setStatus(approved ? WikiEntryStatusCode.ACTIVE : WikiEntryStatusCode.REJECTED);
    entry.setConfidence(approved ? BigDecimal.ONE : entry.getConfidence());
    entry.setVersion(entry.getVersion() + 1);
    entry = wikiEntryRepository.save(entry);
    saveHistory(entry, "USER", approved ? "CONFIRMED" : "REJECTED");
    return toView(entry);
  }

  private WikiEntryEntity createCandidate(Long userSq, Long messageSq, String summary) {
    WikiEntryEntity entry = new WikiEntryEntity();
    entry.setUserSq(userSq);
    entry.setCategory(inferCategory(summary));
    entry.setEntryKey("CHAT_" + messageSq);
    entry.setSummary(summary);
    entry.setContentJson(Map.of("fact", summary));
    entry.setSourceType("CHAT");
    entry.setSourceRefSq(messageSq);
    entry.setConfidence(new BigDecimal("0.7000"));
    entry.setStatus(WikiEntryStatusCode.PENDING_CONFIRMATION);
    entry.setSensitivityLevel(WikiSensitivityCode.NORMAL);
    entry.setVersion(1);
    entry = wikiEntryRepository.save(entry);
    saveHistory(entry, "PERSONAL_WIKI", "CHAT_PROPOSAL");
    return entry;
  }

  private WikiEntryEntity ownedEntry(Long userSq, Long wikiEntrySq) {
    return wikiEntryRepository.findByWikiEntrySqAndUserSq(wikiEntrySq, userSq)
        .orElseThrow(() -> new APIException(DATA_NOT_EXIST));
  }

  private void saveHistory(WikiEntryEntity entry, String changedBy, String reason) {
    WikiEntryHistoryEntity history = new WikiEntryHistoryEntity();
    history.setWikiEntrySq(entry.getWikiEntrySq());
    history.setUserSq(entry.getUserSq());
    history.setVersion(entry.getVersion());
    history.setSnapshotJson(snapshot(entry));
    history.setChangedBy(changedBy);
    history.setChangeReason(reason);
    wikiEntryHistoryRepository.save(history);
  }

  private Map<String, Object> snapshot(WikiEntryEntity entry) {
    Map<String, Object> snapshot = new HashMap<>();
    snapshot.put("category", entry.getCategory());
    snapshot.put("entryKey", entry.getEntryKey());
    snapshot.put("summary", entry.getSummary());
    snapshot.put("content", entry.getContentJson());
    snapshot.put("sourceType", entry.getSourceType());
    snapshot.put("confidence", entry.getConfidence());
    snapshot.put("status", entry.getStatus().name());
    snapshot.put("sensitivityLevel", entry.getSensitivityLevel().name());
    snapshot.put("validUntil", entry.getValidUntil());
    return snapshot;
  }

  private WikiEntryView toView(WikiEntryEntity entry) {
    return new WikiEntryView(
        entry.getWikiEntrySq(),
        entry.getCategory(),
        entry.getEntryKey(),
        entry.getSummary(),
        entry.getContentJson(),
        entry.getSourceType(),
        entry.getConfidence(),
        entry.getStatus().name(),
        entry.getSensitivityLevel().name(),
        entry.getVersion(),
        entry.getValidUntil(),
        entry.getCreateDate(),
        entry.getModifiedDate()
    );
  }

  private boolean matches(WikiEntryEntity entry, String query) {
    if (query == null) {
      return true;
    }
    String searchable = (entry.getCategory() + " " + entry.getEntryKey() + " "
        + entry.getSummary() + " " + entry.getContentJson()).toLowerCase(Locale.ROOT);
    return searchable.contains(query.toLowerCase(Locale.ROOT));
  }

  private boolean isRememberRequest(String text) {
    return containsAny(text, "기억해", "기억해줘", "저장해", "위키에 추가", "remember");
  }

  private boolean looksImportant(String text) {
    return containsAny(text,
        "나는 ", "저는 ", "우리 집", "키우고", "반려동물", "선호해", "좋아해", "싫어해",
        "자주 구매", "항상 구매", "중요하게 생각");
  }

  private String extractRememberedText(String text) {
    String result = text;
    for (String command : List.of("기억해줘", "기억해", "저장해줘", "저장해", "위키에 추가해줘", "위키에 추가")) {
      result = result.replace(command, "");
    }
    return result.replaceAll("^[,:\\s]+|[,:\\s]+$", "").trim();
  }

  private String extractQuery(String text) {
    String result = text;
    for (String command : List.of("위키", "기억", "알려줘", "찾아줘", "조회해줘", "보여줘")) {
      result = result.replace(command, " ");
    }
    result = result.replaceAll("\\s+", " ").trim();
    return result.isBlank() ? null : result;
  }

  private String inferCategory(String text) {
    if (containsAny(text, "고양이", "강아지", "반려", "사료", "간식")) {
      return "PET";
    }
    if (containsAny(text, "구매", "쇼핑", "배송", "브랜드", "가격")) {
      return "SHOPPING";
    }
    if (containsAny(text, "가족", "가구", "집", "생활")) {
      return "HOUSEHOLD";
    }
    return "OTHER";
  }

  private void rejectRestricted(String text) {
    if (containsRestricted(text)) {
      throw new APIException(BAD_REQUEST);
    }
  }

  private boolean containsRestricted(String text) {
    String normalizedText = text.toLowerCase(Locale.ROOT);
    for (String keyword : RESTRICTED_KEYWORDS) {
      if (normalizedText.contains(keyword.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private boolean containsAny(String text, String... keywords) {
    String normalized = text.toLowerCase(Locale.ROOT);
    for (String keyword : keywords) {
      if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private String normalizeRequired(String value) {
    return value.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : normalizeRequired(value);
  }

  private String normalizeKey(String value) {
    return normalizeRequired(value).replaceAll("[^\\p{L}\\p{N}_-]+", "_");
  }

  public record WikiView(List<WikiEntryView> entries, int totalCount, String status) {
  }

  public record WikiEntryView(
      Long wikiEntrySq,
      String category,
      String entryKey,
      String summary,
      Map<String, Object> content,
      String sourceType,
      BigDecimal confidence,
      String status,
      String sensitivityLevel,
      Integer version,
      LocalDate validUntil,
      LocalDateTime createdAt,
      LocalDateTime modifiedAt
  ) {
  }

  public record WikiAgentReply(
      AgentMessageTypeCode messageType,
      String text,
      Map<String, Object> payload,
      List<Map<String, Object>> actions,
      Long candidateWikiEntrySq
  ) {
  }
}
