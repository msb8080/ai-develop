package dev.rainbow.aidevelop.run;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AgentRunRepository extends JpaRepository<AgentRunEntity, UUID> {
}
