package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.AgentRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentRunRepository extends JpaRepository<AgentRunEntity, Long> {

  List<AgentRunEntity> findAllByAgentSessionSqAndUserSqOrderByAgentRunSqAsc(
      Long agentSessionSq,
      Long userSq
  );
}
