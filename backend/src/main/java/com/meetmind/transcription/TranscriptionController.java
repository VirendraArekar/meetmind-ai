package com.meetmind.transcription;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Transcription", description = "AI audio transcription, diarization, and transcript editing")
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    @PostMapping("/{id}/transcribe")
    @Operation(summary = "Transcribe uploaded meeting audio/video and index for RAG")
    public ResponseEntity<ApiResponse<TranscriptionService.TranscriptDto>> transcribe(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Transcription completed",
                transcriptionService.transcribeMeeting(SecurityUtils.currentUserId(), id)
        ));
    }

    @GetMapping("/{id}/transcript")
    @Operation(summary = "Get timestamped meeting transcript")
    public ResponseEntity<ApiResponse<TranscriptionService.TranscriptDto>> getTranscript(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                transcriptionService.getTranscript(SecurityUtils.currentUserId(), id)
        ));
    }

    @PutMapping("/{id}/transcript")
    @Operation(summary = "Edit transcript segments and re-index embeddings")
    public ResponseEntity<ApiResponse<TranscriptionService.TranscriptDto>> updateTranscript(
            @PathVariable UUID id,
            @Valid @RequestBody TranscriptionService.UpdateTranscriptRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Transcript updated",
                transcriptionService.updateTranscript(SecurityUtils.currentUserId(), id, request)
        ));
    }
}
