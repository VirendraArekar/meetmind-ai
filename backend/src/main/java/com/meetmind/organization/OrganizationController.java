package com.meetmind.organization;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations & Teams", description = "Workspace members, invitations, and teams")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/current")
    @Operation(summary = "Get current workspace details, members, and teams")
    public ResponseEntity<ApiResponse<OrganizationService.OrgDto>> getCurrent() {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getMyOrganization(SecurityUtils.currentUserId())));
    }

    @PostMapping("/current/members")
    @Operation(summary = "Add an existing user to the current workspace")
    public ResponseEntity<ApiResponse<OrganizationService.MemberDto>> inviteMember(
            @Valid @RequestBody OrganizationService.InviteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Member added to workspace",
                        organizationService.inviteMember(SecurityUtils.currentUserId(), request)));
    }

    @PostMapping("/current/teams")
    @Operation(summary = "Create a team in the current workspace")
    public ResponseEntity<ApiResponse<OrganizationService.TeamDto>> createTeam(
            @Valid @RequestBody OrganizationService.CreateTeamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Team created",
                        organizationService.createTeam(SecurityUtils.currentUserId(), request)));
    }
}
