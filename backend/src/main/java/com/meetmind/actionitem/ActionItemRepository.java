package com.meetmind.actionitem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionItemRepository extends JpaRepository<ActionItem, UUID> {
    List<ActionItem> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<ActionItem> findByMeetingIdOrderByCreatedAtAsc(UUID meetingId);
    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
}
