package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.WikiEntryEntity;
import com.foryour.delivery.domain.enums.WikiEntryStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WikiEntryRepository extends JpaRepository<WikiEntryEntity, Long> {

  Optional<WikiEntryEntity> findByWikiEntrySqAndUserSq(Long wikiEntrySq, Long userSq);

  Optional<WikiEntryEntity> findByUserSqAndCategoryAndEntryKey(
      Long userSq,
      String category,
      String entryKey
  );

  List<WikiEntryEntity> findAllByUserSqAndStatusOrderByModifiedDateDesc(
      Long userSq,
      WikiEntryStatusCode status
  );
}
