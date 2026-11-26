package com.meetmind.summary;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "AI Summaries", description = "Structured AI meeting summarization and decision/action-item extraction")
public class SummaryController {

    private final SummaryService summaryService;

    @PostMapping("/{id}/summarize")
    @Operation(summary = "Generate structured AI summary (SHORT, DETAILED, or EXECUTIVE)")
    public ResponseEntity<ApiResponse<SummaryService.SummaryDto>> summarize(
            @PathVariable UUID id,
            @RequestBody(required = false) SummaryService.SummarizeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Summary generated",
                summaryService.summarize(SecurityUtils.currentUserId(), id, request)
        ));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Get structured AI summary for a meeting")
    public ResponseEntity<ApiResponse<SummaryService.SummaryDto>> getSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                summaryService.getSummary(SecurityUtils.currentUserId(), id)
        ));
    }
}
