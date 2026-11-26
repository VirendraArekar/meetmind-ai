package com.meetmind.auth;

import com.meetmind.common.audit.AuditService;
import com.meetmind.common.exception.BadRequestException;
import com.meetmind.common.exception.ResourceNotFoundException;
import com.meetmind.organization.Organization;
import com.meetmind.organization.OrganizationMember;
import com.meetmind.organization.OrganizationMemberRepository;
import com.meetmind.organization.OrganizationRepository;
import com.meetmind.security.JwtService;
import com.meetmind.user.Role;
import com.meetmind.user.RoleRepository;
import com.meetmind.user.User;
import com.meetmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BadRequestException("An account with this email already exists");
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not initialized"));

        User user = User.builder()
                .email(req.email().trim().toLowerCase(Locale.ROOT))
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName().trim())
                .emailVerified(false)
                .emailVerificationToken(UUID.randomUUID().toString())
                .roles(Set.of(adminRole))
                .build();
        user = userRepository.save(user);

        String orgName = (req.organizationName() != null && !req.organizationName().isBlank())
                ? req.organizationName().trim()
                : req.fullName().trim() + "'s Workspace";
        String slug = orgName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                + "-" + UUID.randomUUID().toString().substring(0, 6);

        Organization org = organizationRepository.save(Organization.builder()
                .name(orgName)
                .slug(slug)
                .owner(user)
                .build());

        organizationMemberRepository.save(OrganizationMember.builder()
                .organization(org)
                .user(user)
                .workspaceRole("ADMIN")
                .invitationStatus("ACTIVE")
                .build());

        auditService.log(user.getId(), org.getId(), "USER_REGISTERED", "USER", user.getId().toString(), user.getEmail());
        return issueTokens(user, org);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(req.email().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isActive() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        Organization org = resolveDefaultOrganization(user);
        auditService.log(user.getId(), org != null ? org.getId() : null, "USER_LOGIN", "USER", user.getId().toString(), user.getEmail());
        return issueTokens(user, org);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest req) {
        String tokenHash = JwtService.sha256Hex(req.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired or revoked");
        }

        String newRawRefresh = jwtService.generateRefreshTokenValue();
        String newHash = JwtService.sha256Hex(newRawRefresh);

        existing.setRevoked(true);
        existing.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(existing);

        User user = existing.getUser();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .build());

        Organization org = resolveDefaultOrganization(user);
        return new AuthDtos.AuthResponse(
                jwtService.generateAccessToken(user),
                newRawRefresh,
                "Bearer",
                toUserSummary(user, org)
        );
    }

    @Transactional
    public void logout(UUID userId, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(JwtService.sha256Hex(rawRefreshToken))
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        }
        auditService.log(userId, null, "USER_LOGOUT", "USER", userId.toString(), "User logged out");
    }

    @Transactional
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No active account found for this email"));
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        userRepository.save(user);
        return token;
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {
        User user = userRepository.findByPasswordResetToken(req.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token has expired");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserSummary getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserSummary(user, resolveDefaultOrganization(user));
    }

    @Transactional
    public AuthDtos.UserSummary updateProfile(UUID userId, AuthDtos.UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFullName(req.fullName().trim());
        user.setJobTitle(req.jobTitle());
        user.setAvatarUrl(req.avatarUrl());
        user = userRepository.save(user);
        return toUserSummary(user, resolveDefaultOrganization(user));
    }

    private AuthDtos.AuthResponse issueTokens(User user, Organization org) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefresh = jwtService.generateRefreshTokenValue();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(JwtService.sha256Hex(rawRefresh))
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .build());
        return new AuthDtos.AuthResponse(accessToken, rawRefresh, "Bearer", toUserSummary(user, org));
    }

    private Organization resolveDefaultOrganization(User user) {
        List<OrganizationMember> memberships = organizationMemberRepository.findByUserId(user.getId());
        if (!memberships.isEmpty()) {
            return memberships.getFirst().getOrganization();
        }
        return null;
    }

    private AuthDtos.UserSummary toUserSummary(User user, Organization org) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        return new AuthDtos.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getJobTitle(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                roles,
                org != null ? org.getId() : null,
                org != null ? org.getName() : null
        );
    }
}
