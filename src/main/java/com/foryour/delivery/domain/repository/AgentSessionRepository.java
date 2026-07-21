package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.AgentSessionEntity;
import com.foryour.delivery.domain.enums.AgentSessionStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentSessionRepository extends JpaRepository<AgentSessionEntity, Long> {

  Optional<AgentSessionEntity> findByAgentSessionSqAndUserSq(Long agentSessionSq, Long userSq);

  List<AgentSessionEntity> findAllByUserSqAndStatusOrderByLastMessageAtDesc(
      Long userSq,
      AgentSessionStatusCode status
  );
}
