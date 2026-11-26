package com.meetmind.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(UUID userId, UUID organizationId, String action, String resourceType, String resourceId, String details) {
        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .organizationId(organizationId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }
}
