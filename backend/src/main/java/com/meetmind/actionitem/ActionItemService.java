package com.meetmind.actionitem;

import com.meetmind.common.exception.ResourceNotFoundException;
import com.meetmind.meeting.Meeting;
import com.meetmind.meeting.MeetingService;
import com.meetmind.organization.OrganizationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActionItemService {

    public record ActionItemDto(
            UUID id,
            UUID meetingId,
            String title,
            String description,
            String assignedToName,
            String priority,
            LocalDate dueDate,
            String status,
            String transcriptTimestamp,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CreateActionItemRequest(
            @NotNull UUID meetingId,
            @NotBlank String title,
            String description,
            String assignedToName,
            String priority,
            LocalDate dueDate,
            String status,
            String transcriptTimestamp
    ) {}

    public record UpdateActionItemRequest(
            @NotBlank String title,
            String description,
            String assignedToName,
            String priority,
            LocalDate dueDate,
            String status,
            String transcriptTimestamp
    ) {}

    private final ActionItemRepository actionItemRepository;
    private final MeetingService meetingService;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public List<ActionItemDto> listActionItems(UUID userId, UUID meetingId, String status) {
        if (meetingId != null) {
            meetingService.requireAccessibleMeeting(userId, meetingId);
            return actionItemRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId).stream()
                    .filter(item -> status == null || status.isBlank() || item.getStatus().equalsIgnoreCase(status))
                    .map(this::toDto)
                    .toList();
        }
        UUID orgId = organizationService.requireDefaultOrganizationId(userId);
        return actionItemRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .filter(item -> status == null || status.isBlank() || item.getStatus().equalsIgnoreCase(status))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ActionItemDto create(UUID userId, CreateActionItemRequest req) {
        Meeting meeting = meetingService.requireAccessibleMeeting(userId, req.meetingId());
        ActionItem saved = actionItemRepository.save(ActionItem.builder()
                .meetingId(meeting.getId())
                .organizationId(meeting.getOrganizationId())
                .title(req.title().trim())
                .description(req.description())
                .assignedToName(req.assignedToName())
                .priority(normalize(req.priority(), "MEDIUM"))
                .dueDate(req.dueDate())
                .status(normalize(req.status(), "PENDING"))
                .transcriptTimestamp(req.transcriptTimestamp())
                .build());
        return toDto(saved);
    }

    @Transactional
    public ActionItemDto update(UUID userId, UUID id, UpdateActionItemRequest req) {
        ActionItem item = actionItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action item not found"));
        meetingService.requireAccessibleMeeting(userId, item.getMeetingId());

        item.setTitle(req.title().trim());
        item.setDescription(req.description());
        item.setAssignedToName(req.assignedToName());
        if (req.priority() != null) {
            item.setPriority(normalize(req.priority(), "MEDIUM"));
        }
        item.setDueDate(req.dueDate());
        if (req.status() != null) {
            item.setStatus(normalize(req.status(), "PENDING"));
        }
        if (req.transcriptTimestamp() != null) {
            item.setTranscriptTimestamp(req.transcriptTimestamp());
        }
        return toDto(actionItemRepository.save(item));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        ActionItem item = actionItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action item not found"));
        meetingService.requireAccessibleMeeting(userId, item.getMeetingId());
        actionItemRepository.delete(item);
    }

    private String normalize(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    public ActionItemDto toDto(ActionItem item) {
        return new ActionItemDto(
                item.getId(),
                item.getMeetingId(),
                item.getTitle(),
                item.getDescription(),
                item.getAssignedToName(),
                item.getPriority(),
                item.getDueDate(),
                item.getStatus(),
                item.getTranscriptTimestamp(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
