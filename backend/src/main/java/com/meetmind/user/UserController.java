package com.meetmind.user;

import com.meetmind.auth.AuthDtos;
import com.meetmind.auth.AuthService;
import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Authenticated user profile management")
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<AuthDtos.UserSummary>> getMe() {
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(SecurityUtils.currentUserId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<AuthDtos.UserSummary>> updateMe(
            @Valid @RequestBody AuthDtos.UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Profile updated",
                authService.updateProfile(SecurityUtils.currentUserId(), request)
        ));
    }
}
