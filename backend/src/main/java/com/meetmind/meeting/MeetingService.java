package com.meetmind.meeting;

import com.meetmind.common.audit.AuditService;
import com.meetmind.common.exception.ApiException;
import com.meetmind.common.exception.BadRequestException;
import com.meetmind.common.exception.ResourceNotFoundException;
import com.meetmind.organization.OrganizationService;
import com.meetmind.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final long MAX_FILE_SIZE_BYTES = 500L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "wav", "m4a", "mp4", "webm");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/wave",
            "audio/mp4", "audio/x-m4a", "audio/m4a", "video/mp4",
            "audio/webm", "video/webm", "application/octet-stream"
    );

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingFileRepository fileRepository;
    private final OrganizationService organizationService;
    private final StorageService storageService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<MeetingDtos.MeetingDto> listMeetings(
            UUID userId,
            String category,
            Boolean archived,
            Pageable pageable
    ) {
        UUID orgId = organizationService.requireDefaultOrganizationId(userId);
        String catFilter = (category != null && !category.isBlank()) ? category.trim().toUpperCase(Locale.ROOT) : null;
        return meetingRepository.findAccessibleMeetings(orgId, userId, catFilter, archived, pageable)
                .map(this::toDto);
    }

    @Transactional
    public MeetingDtos.MeetingDto createMeeting(UUID userId, MeetingDtos.CreateMeetingRequest req) {
        UUID orgId = organizationService.requireDefaultOrganizationId(userId);

        Meeting meeting = Meeting.builder()
                .organizationId(orgId)
                .createdBy(userId)
                .title(req.title().trim())
                .description(req.description())
                .category(normalizeOrDefault(req.category(), "GENERAL"))
                .visibility(normalizeVisibility(req.visibility()))
                .sourceType(normalizeOrDefault(req.sourceType(), "UPLOAD"))
                .scheduledAt(req.scheduledAt())
                .durationSeconds(req.durationSeconds() != null ? Math.max(0, req.durationSeconds()) : 0)
                .status("CREATED")
                .build();
        meeting = meetingRepository.save(meeting);

        if (req.participants() != null) {
            saveParticipants(meeting.getId(), req.participants());
        }

        auditService.log(userId, orgId, "MEETING_CREATED", "MEETING", meeting.getId().toString(), meeting.getTitle());
        return toDto(meeting);
    }

    @Transactional(readOnly = true)
    public MeetingDtos.MeetingDto getMeeting(UUID userId, UUID meetingId) {
        Meeting meeting = requireAccessibleMeeting(userId, meetingId);
        return toDto(meeting);
    }

    @Transactional
    public MeetingDtos.MeetingDto updateMeeting(UUID userId, UUID meetingId, MeetingDtos.UpdateMeetingRequest req) {
        Meeting meeting = requireAccessibleMeeting(userId, meetingId);
        meeting.setTitle(req.title().trim());
        meeting.setDescription(req.description());
        if (req.category() != null) {
            meeting.setCategory(normalizeOrDefault(req.category(), "GENERAL"));
        }
        if (req.visibility() != null) {
            meeting.setVisibility(normalizeVisibility(req.visibility()));
        }
        if (req.archived() != null) {
            meeting.setArchived(req.archived());
        }
        if (req.durationSeconds() != null) {
            meeting.setDurationSeconds(Math.max(0, req.durationSeconds()));
        }
        meeting = meetingRepository.save(meeting);

        if (req.participants() != null) {
            participantRepository.deleteByMeetingId(meeting.getId());
            saveParticipants(meeting.getId(), req.participants());
        }

        return toDto(meeting);
    }

    @Transactional
    public void deleteMeeting(UUID userId, UUID meetingId) {
        Meeting meeting = requireAccessibleMeeting(userId, meetingId);
        if (!meeting.getCreatedBy().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the meeting creator can delete this meeting") {};
        }
        meeting.setDeletedAt(Instant.now());
        meetingRepository.save(meeting);
        auditService.log(userId, meeting.getOrganizationId(), "MEETING_DELETED", "MEETING", meetingId.toString(), meeting.getTitle());
    }

    @Transactional
    public MeetingDtos.MeetingFileDto uploadMedia(UUID userId, UUID meetingId, MultipartFile file, Integer durationSeconds) {
        Meeting meeting = requireAccessibleMeeting(userId, meetingId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Audio/video file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File exceeds maximum allowed size of 500MB");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "recording.webm";
        String ext = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BadRequestException("Unsupported file format. Allowed: MP3, WAV, M4A, MP4, WEBM");
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "application/octet-stream";
        String baseMime = contentType.contains(";") ? contentType.substring(0, contentType.indexOf(';')).trim() : contentType;
        if (!ALLOWED_MIME_TYPES.contains(baseMime)) {
            throw new BadRequestException("Invalid media MIME type: " + baseMime);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read uploaded file", e);
        }

        StorageService.StoredObject stored = storageService.store(
                meeting.getOrganizationId(), meeting.getId(), originalName, baseMime, bytes);

        MeetingFile savedFile = fileRepository.save(MeetingFile.builder()
                .meetingId(meeting.getId())
                .originalFilename(originalName)
                .contentType(baseMime)
                .fileSizeBytes(file.getSize())
                .storageKey(stored.storageKey())
                .storageProvider(stored.provider())
                .durationSeconds(durationSeconds != null ? Math.max(0, durationSeconds) : 0)
                .build());

        if (durationSeconds != null && durationSeconds > 0) {
            meeting.setDurationSeconds(durationSeconds);
        }
        meeting.setStatus("UPLOADED");
        meetingRepository.save(meeting);

        auditService.log(userId, meeting.getOrganizationId(), "MEDIA_UPLOADED", "MEETING", meetingId.toString(), originalName);
        return toFileDto(savedFile);
    }

    public Meeting requireAccessibleMeeting(UUID userId, UUID meetingId) {
        Meeting meeting = meetingRepository.findByIdAndDeletedAtIsNull(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        organizationService.requireMembership(meeting.getOrganizationId(), userId);

        if ("PRIVATE".equalsIgnoreCase(meeting.getVisibility()) && !meeting.getCreatedBy().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this private meeting") {};
        }
        return meeting;
    }

    private void saveParticipants(UUID meetingId, List<MeetingDtos.ParticipantInput> inputs) {
        for (MeetingDtos.ParticipantInput input : inputs) {
            participantRepository.save(MeetingParticipant.builder()
                    .meetingId(meetingId)
                    .displayName(input.displayName().trim())
                    .email(input.email())
                    .role(normalizeOrDefault(input.role(), "PARTICIPANT"))
                    .build());
        }
    }

    private String normalizeVisibility(String visibility) {
        String v = normalizeOrDefault(visibility, "PRIVATE");
        return ("SHARED".equals(v)) ? "SHARED" : "PRIVATE";
    }

    private String normalizeOrDefault(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private MeetingDtos.MeetingDto toDto(Meeting m) {
        List<MeetingDtos.ParticipantDto> participants = participantRepository.findByMeetingId(m.getId()).stream()
                .map(p -> new MeetingDtos.ParticipantDto(p.getId(), p.getDisplayName(), p.getEmail(), p.getRole()))
                .toList();
        List<MeetingDtos.MeetingFileDto> files = fileRepository.findByMeetingIdOrderByCreatedAtDesc(m.getId()).stream()
                .map(this::toFileDto)
                .toList();
        return new MeetingDtos.MeetingDto(
                m.getId(),
                m.getOrganizationId(),
                m.getCreatedBy(),
                m.getTitle(),
                m.getDescription(),
                m.getCategory(),
                m.getVisibility(),
                m.getStatus(),
                m.getSourceType(),
                m.getScheduledAt(),
                m.getDurationSeconds(),
                m.isArchived(),
                m.getCreatedAt(),
                m.getUpdatedAt(),
                participants,
                files
        );
    }

    private MeetingDtos.MeetingFileDto toFileDto(MeetingFile f) {
        return new MeetingDtos.MeetingFileDto(
                f.getId(),
                f.getOriginalFilename(),
                f.getContentType(),
                f.getFileSizeBytes(),
                f.getStorageProvider(),
                f.getCreatedAt()
        );
    }
}
