package com.meetmind.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiUsageLogRepository repository;

    public void record(UUID organizationId, UUID meetingId, UUID userId,
                       String operation, String provider, String model,
                       int tokensUsed, long latencyMs, boolean success) {
        repository.save(AiUsageLog.builder()
                .organizationId(organizationId)
                .meetingId(meetingId)
                .userId(userId)
                .operation(operation)
                .provider(provider)
                .model(model)
                .tokensUsed(tokensUsed)
                .latencyMs(latencyMs)
                .success(success)
                .build());
    }
}
