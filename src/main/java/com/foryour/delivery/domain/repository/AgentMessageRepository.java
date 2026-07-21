package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.AgentMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, Long> {

  List<AgentMessageEntity> findAllByAgentSessionSqAndUserSqOrderByAgentMessageSqAsc(
      Long agentSessionSq,
      Long userSq
  );
}
