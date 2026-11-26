package com.meetmind.meeting;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "Meeting management and audio/video upload endpoints")
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    @Operation(summary = "List accessible meetings in the current workspace")
    public ResponseEntity<ApiResponse<Page<MeetingDtos.MeetingDto>>> listMeetings(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean archived,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                meetingService.listMeetings(SecurityUtils.currentUserId(), category, archived, pageable)
        ));
    }

    @PostMapping
    @Operation(summary = "Create a new meeting")
    public ResponseEntity<ApiResponse<MeetingDtos.MeetingDto>> createMeeting(
            @Valid @RequestBody MeetingDtos.CreateMeetingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Meeting created",
                        meetingService.createMeeting(SecurityUtils.currentUserId(), request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meeting details")
    public ResponseEntity<ApiResponse<MeetingDtos.MeetingDto>> getMeeting(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                meetingService.getMeeting(SecurityUtils.currentUserId(), id)
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update meeting metadata or participants")
    public ResponseEntity<ApiResponse<MeetingDtos.MeetingDto>> updateMeeting(
            @PathVariable UUID id,
            @Valid @RequestBody MeetingDtos.UpdateMeetingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Meeting updated",
                meetingService.updateMeeting(SecurityUtils.currentUserId(), id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a meeting")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(@PathVariable UUID id) {
        meetingService.deleteMeeting(SecurityUtils.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Meeting deleted", null));
    }

    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload meeting audio or video recording (MP3, WAV, M4A, MP4, WEBM)")
    public ResponseEntity<ApiResponse<MeetingDtos.MeetingFileDto>> uploadMedia(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Integer durationSeconds
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Media uploaded",
                        meetingService.uploadMedia(SecurityUtils.currentUserId(), id, file, durationSeconds)));
    }
}
