package com.meetmind.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "meeting_id")
    private UUID meetingId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 60)
    private String operation;

    @Column(nullable = false, length = 60)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "tokens_used", nullable = false)
    @Builder.Default
    private int tokensUsed = 0;

    @Column(name = "latency_ms", nullable = false)
    @Builder.Default
    private long latencyMs = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
