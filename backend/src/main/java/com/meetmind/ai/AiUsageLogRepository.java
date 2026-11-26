package com.meetmind.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {
    List<AiUsageLog> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}
