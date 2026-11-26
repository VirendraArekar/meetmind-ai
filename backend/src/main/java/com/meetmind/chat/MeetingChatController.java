package com.meetmind.chat;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "AI Meeting Chat", description = "RAG-grounded question answering over meeting transcripts")
public class MeetingChatController {

    private final MeetingChatService chatService;

    @PostMapping("/{id}/chat")
    @Operation(summary = "Ask a question about a meeting transcript using RAG")
    public ResponseEntity<ApiResponse<MeetingChatService.ChatResponseDto>> ask(
            @PathVariable UUID id,
            @Valid @RequestBody MeetingChatService.ChatRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.ask(SecurityUtils.currentUserId(), id, request)
        ));
    }

    @GetMapping("/{id}/chat")
    @Operation(summary = "Get chat history for a meeting")
    public ResponseEntity<ApiResponse<List<MeetingChatService.ChatMessageDto>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.getHistory(SecurityUtils.currentUserId(), id)
        ));
    }
}
