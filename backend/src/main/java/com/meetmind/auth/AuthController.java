package com.meetmind.auth;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, JWT refresh, password reset, and email verification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user and default workspace")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access token")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> refresh(
            @Valid @RequestBody AuthDtos.RefreshRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token and log out")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) AuthDtos.LogoutRequest request
    ) {
        authService.logout(
                SecurityUtils.currentUserId(),
                request != null ? request.refreshToken() : null
        );
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset token")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(
            @Valid @RequestBody AuthDtos.ForgotPasswordRequest request
    ) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.ok(
                "If an account exists, password reset instructions have been issued",
                Map.of("status", "REQUESTED")
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody AuthDtos.ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully", null));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address using verification token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody AuthDtos.VerifyEmailRequest request
    ) {
        authService.verifyEmail(request.token());
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", null));
    }
}
