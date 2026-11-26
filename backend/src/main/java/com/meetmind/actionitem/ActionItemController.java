package com.meetmind.actionitem;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/action-items")
@RequiredArgsConstructor
@Tag(name = "Action Items", description = "Manage extracted and manual meeting tasks")
public class ActionItemController {

    private final ActionItemService actionItemService;

    @GetMapping
    @Operation(summary = "List action items for the workspace or a specific meeting")
    public ResponseEntity<ApiResponse<List<ActionItemService.ActionItemDto>>> list(
            @RequestParam(required = false) UUID meetingId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                actionItemService.listActionItems(SecurityUtils.currentUserId(), meetingId, status)
        ));
    }

    @PostMapping
    @Operation(summary = "Create an action item")
    public ResponseEntity<ApiResponse<ActionItemService.ActionItemDto>> create(
            @Valid @RequestBody ActionItemService.CreateActionItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Action item created",
                        actionItemService.create(SecurityUtils.currentUserId(), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an action item")
    public ResponseEntity<ApiResponse<ActionItemService.ActionItemDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ActionItemService.UpdateActionItemRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Action item updated",
                actionItemService.update(SecurityUtils.currentUserId(), id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an action item")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        actionItemService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Action item deleted", null));
    }
}
