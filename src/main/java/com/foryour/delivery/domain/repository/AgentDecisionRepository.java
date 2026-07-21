package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.AgentDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentDecisionRepository extends JpaRepository<AgentDecisionEntity, Long> {

  List<AgentDecisionEntity> findAllByAgentRunSqOrderByAgentDecisionSqAsc(Long agentRunSq);
}
