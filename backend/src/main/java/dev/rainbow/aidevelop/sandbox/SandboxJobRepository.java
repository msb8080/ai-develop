package dev.rainbow.aidevelop.sandbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SandboxJobRepository extends JpaRepository<SandboxJobEntity, UUID> {
    List<SandboxJobEntity> findTop20ByOrderByRequestedAtDesc();
}
