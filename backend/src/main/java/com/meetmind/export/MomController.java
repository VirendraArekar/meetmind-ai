package com.meetmind.export;

import com.meetmind.common.exception.BadRequestException;
import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "MOM & Exports", description = "Minutes of Meeting generation and PDF/DOCX/TXT exports")
public class MomController {

    private final MomExportService momExportService;

    @PostMapping("/{id}/generate-mom")
    @Operation(summary = "Generate Minutes of Meeting (MOM)")
    public ResponseEntity<ApiResponse<MomExportService.MomDto>> generateMom(
            @PathVariable UUID id,
            @RequestBody(required = false) MomExportService.GenerateMomRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Minutes of Meeting generated",
                momExportService.generateMom(SecurityUtils.currentUserId(), id, request)
        ));
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Export MOM report as PDF, DOCX, or TXT")
    public ResponseEntity<byte[]> exportMom(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "pdf") String format
    ) {
        UUID userId = SecurityUtils.currentUserId();
        String fmt = format.trim().toLowerCase(Locale.ROOT);
        return switch (fmt) {
            case "pdf" -> ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meeting-" + id + "-mom.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(momExportService.exportPdf(userId, id));
            case "docx" -> ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meeting-" + id + "-mom.docx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(momExportService.exportDocx(userId, id));
            case "txt" -> ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meeting-" + id + "-mom.txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(momExportService.exportTxt(userId, id));
            default -> throw new BadRequestException("Unsupported export format. Allowed: pdf, docx, txt");
        };
    }
}
