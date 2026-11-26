package com.meetmind.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(min = 2, max = 120) String fullName,
            String organizationName
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}

    public record LogoutRequest(
            String refreshToken
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}

    public record VerifyEmailRequest(
            @NotBlank String token
    ) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            String jobTitle,
            String avatarUrl
    ) {}

    public record UserSummary(
            UUID id,
            String email,
            String fullName,
            String jobTitle,
            String avatarUrl,
            boolean emailVerified,
            List<String> roles,
            UUID defaultOrganizationId,
            String defaultOrganizationName
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            UserSummary user
    ) {}
}
