package com.meetmind.meeting;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 80)
    @Builder.Default
    private String category = "GENERAL";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String visibility = "PRIVATE";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "CREATED";

    @Column(name = "source_type", nullable = false, length = 30)
    @Builder.Default
    private String sourceType = "UPLOAD";

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "duration_seconds", nullable = false)
    @Builder.Default
    private int durationSeconds = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
