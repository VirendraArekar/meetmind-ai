package com.meetmind.meeting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MeetingDtos {

    private MeetingDtos() {}

    public record ParticipantInput(
            @NotBlank String displayName,
            String email,
            String role
    ) {}

    public record CreateMeetingRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            String category,
            String visibility,
            String sourceType,
            Instant scheduledAt,
            Integer durationSeconds,
            List<ParticipantInput> participants
    ) {}

    public record UpdateMeetingRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            String category,
            String visibility,
            Boolean archived,
            Integer durationSeconds,
            List<ParticipantInput> participants
    ) {}

    public record ParticipantDto(
            UUID id,
            String displayName,
            String email,
            String role
    ) {}

    public record MeetingFileDto(
            UUID id,
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            String storageProvider,
            Instant createdAt
    ) {}

    public record MeetingDto(
            UUID id,
            UUID organizationId,
            UUID createdBy,
            String title,
            String description,
            String category,
            String visibility,
            String status,
            String sourceType,
            Instant scheduledAt,
            int durationSeconds,
            boolean archived,
            Instant createdAt,
            Instant updatedAt,
            List<ParticipantDto> participants,
            List<MeetingFileDto> files
    ) {}
}
