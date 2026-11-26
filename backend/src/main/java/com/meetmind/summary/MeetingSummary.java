package com.meetmind.summary;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "summary_length", nullable = false, length = 30)
    @Builder.Default
    private String summaryLength = "DETAILED";

    @Column(nullable = false, length = 60)
    private String provider;

    @Column(name = "executive_summary", nullable = false, columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "discussion_points", columnDefinition = "TEXT")
    private String discussionPoints;

    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Column(name = "decisions_summary", columnDefinition = "TEXT")
    private String decisionsSummary;

    @Column(name = "problems_identified", columnDefinition = "TEXT")
    private String problemsIdentified;

    @Column(name = "risks_dependencies", columnDefinition = "TEXT")
    private String risksDependencies;

    @Column(name = "next_steps", columnDefinition = "TEXT")
    private String nextSteps;

    @Column(name = "follow_up_requirements", columnDefinition = "TEXT")
    private String followUpRequirements;

    @Column(name = "mom_agenda", columnDefinition = "TEXT")
    private String momAgenda;

    @Column(name = "mom_next_meeting", columnDefinition = "TEXT")
    private String momNextMeeting;

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
