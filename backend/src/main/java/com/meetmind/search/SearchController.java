package com.meetmind.search;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text and semantic vector search across meetings")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Full-text search across accessible meetings and transcripts")
    public ResponseEntity<ApiResponse<List<SearchService.SearchHitDto>>> search(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                searchService.fullTextSearch(SecurityUtils.currentUserId(), q, category)
        ));
    }

    @PostMapping("/semantic")
    @Operation(summary = "Semantic vector search across indexed meeting transcripts")
    public ResponseEntity<ApiResponse<List<SearchService.SearchHitDto>>> semanticSearch(
            @Valid @RequestBody SearchService.SemanticSearchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                searchService.semanticSearch(SecurityUtils.currentUserId(), request)
        ));
    }
}
