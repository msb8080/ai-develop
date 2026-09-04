package dev.rainbow.aidevelop.run;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AgentEventRepository extends JpaRepository<AgentEventEntity, Long> {
    List<AgentEventEntity> findByRunIdOrderBySequenceNumber(UUID runId);
}
