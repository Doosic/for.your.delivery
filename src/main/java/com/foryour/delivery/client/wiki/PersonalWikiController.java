package com.foryour.delivery.client.wiki;

import com.foryour.delivery.client.wiki.PersonalWikiService.WikiEntryView;
import com.foryour.delivery.client.wiki.PersonalWikiService.WikiView;
import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import com.foryour.delivery.domain.enums.WikiEntryStatusCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PersonalWikiController extends BaseController {

  private final PersonalWikiService personalWikiService;

  @GetMapping("/wb/agent/wiki")
  public APIDataResponse<WikiView> wiki(
      @RequestParam(defaultValue = "ACTIVE") WikiEntryStatusCode status,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String query
  ) {
    return APIDataResponse.of(personalWikiService.wiki(
        getSessionInfo().getUserSq(), status, category, query));
  }

  @PostMapping("/wb/agent/wiki/entries")
  public APIDataResponse<WikiEntryView> createEntry(
      @Valid @RequestBody WikiEntryCreateRequest request
  ) {
    return APIDataResponse.of(personalWikiService.createExplicit(
        getSessionInfo().getUserSq(),
        request.category(),
        request.entryKey(),
        request.summary(),
        request.content()
    ));
  }

  @PostMapping("/wb/agent/wiki/onboarding")
  public APIDataResponse<WikiView> onboarding(
      @Valid @RequestBody WikiOnboardingRequest request
  ) {
    return APIDataResponse.of(personalWikiService.createOnboarding(
        getSessionInfo().getUserSq(),
        request.householdSize(),
        request.pets(),
        request.shoppingPriorities(),
        request.preferredCategories(),
        request.prompt()
    ));
  }

  @PostMapping("/wb/agent/wiki/entries/{wikiEntrySq}/confirm")
  public APIDataResponse<WikiEntryView> confirmEntry(
      @PathVariable Long wikiEntrySq,
      @Valid @RequestBody WikiConfirmRequest request
  ) {
    return APIDataResponse.of(personalWikiService.confirm(
        getSessionInfo().getUserSq(), wikiEntrySq, request.approved()));
  }

  public record WikiEntryCreateRequest(
      @NotBlank @Size(max = 30) String category,
      @NotBlank @Size(max = 100) String entryKey,
      @NotBlank @Size(max = 500) String summary,
      Map<String, Object> content
  ) {
  }

  public record WikiConfirmRequest(boolean approved) {
  }

  public record WikiOnboardingRequest(
      @Min(1) @Max(20) Integer householdSize,
      @Size(max = 10) List<@NotBlank @Size(max = 30) String> pets,
      @Size(max = 10) List<@NotBlank @Size(max = 40) String> shoppingPriorities,
      @Size(max = 20) List<@NotBlank @Size(max = 40) String> preferredCategories,
      @Size(max = 1000) String prompt
  ) {
  }
}
