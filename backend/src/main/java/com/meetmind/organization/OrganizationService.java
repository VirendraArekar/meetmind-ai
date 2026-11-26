package com.meetmind.organization;

import com.meetmind.common.exception.ApiException;
import com.meetmind.common.exception.BadRequestException;
import com.meetmind.common.exception.ResourceNotFoundException;
import com.meetmind.user.User;
import com.meetmind.user.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public record MemberDto(UUID id, UUID userId, String fullName, String email, String workspaceRole, String status) {}
    public record TeamDto(UUID id, String name, String description) {}
    public record OrgDto(UUID id, String name, String slug, String myRole, List<MemberDto> members, List<TeamDto> teams) {}
    public record InviteRequest(@NotBlank @Email String email, String workspaceRole) {}
    public record CreateTeamRequest(@NotBlank String name, String description) {}

    public UUID requireDefaultOrganizationId(UUID userId) {
        List<OrganizationMember> memberships = memberRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("No workspace found for user");
        }
        return memberships.getFirst().getOrganization().getId();
    }

    public void requireMembership(UUID organizationId, UUID userId) {
        if (!memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this organization") {};
        }
    }

    @Transactional(readOnly = true)
    public OrgDto getMyOrganization(UUID userId) {
        UUID orgId = requireDefaultOrganizationId(userId);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        OrganizationMember self = memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        List<MemberDto> members = memberRepository.findByOrganizationId(orgId).stream()
                .map(m -> new MemberDto(
                        m.getId(),
                        m.getUser().getId(),
                        m.getUser().getFullName(),
                        m.getUser().getEmail(),
                        m.getWorkspaceRole(),
                        m.getInvitationStatus()
                ))
                .toList();

        List<TeamDto> teams = teamRepository.findByOrganizationId(orgId).stream()
                .map(t -> new TeamDto(t.getId(), t.getName(), t.getDescription()))
                .toList();

        return new OrgDto(org.getId(), org.getName(), org.getSlug(), self.getWorkspaceRole(), members, teams);
    }

    @Transactional
    public MemberDto inviteMember(UUID actorUserId, InviteRequest req) {
        UUID orgId = requireDefaultOrganizationId(actorUserId);
        OrganizationMember actor = memberRepository.findByOrganizationIdAndUserId(orgId, actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
        if (!"ADMIN".equalsIgnoreCase(actor.getWorkspaceRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only workspace admins can invite members") {};
        }

        User target = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(req.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + req.email() + " must register first"));

        if (memberRepository.existsByOrganizationIdAndUserId(orgId, target.getId())) {
            throw new BadRequestException("User is already a member of this workspace");
        }

        String role = (req.workspaceRole() != null && !req.workspaceRole().isBlank())
                ? req.workspaceRole().trim().toUpperCase(Locale.ROOT)
                : "MEMBER";

        OrganizationMember saved = memberRepository.save(OrganizationMember.builder()
                .organization(actor.getOrganization())
                .user(target)
                .workspaceRole(role)
                .invitationStatus("ACTIVE")
                .invitedEmail(target.getEmail())
                .build());

        return new MemberDto(saved.getId(), target.getId(), target.getFullName(), target.getEmail(), saved.getWorkspaceRole(), saved.getInvitationStatus());
    }

    @Transactional
    public TeamDto createTeam(UUID actorUserId, CreateTeamRequest req) {
        UUID orgId = requireDefaultOrganizationId(actorUserId);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        Team team = teamRepository.save(Team.builder()
                .organization(org)
                .name(req.name().trim())
                .description(req.description())
                .build());
        return new TeamDto(team.getId(), team.getName(), team.getDescription());
    }
}
