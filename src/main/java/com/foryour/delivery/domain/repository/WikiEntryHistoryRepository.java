package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.WikiEntryHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WikiEntryHistoryRepository extends JpaRepository<WikiEntryHistoryEntity, Long> {
}
