package com.meetmind.meeting;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    private String email;

    @Column(length = 50)
    @Builder.Default
    private String role = "PARTICIPANT";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
