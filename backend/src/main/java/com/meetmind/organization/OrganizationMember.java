package com.meetmind.organization;

import com.meetmind.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "organization_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "workspace_role", nullable = false, length = 40)
    @Builder.Default
    private String workspaceRole = "MEMBER";

    @Column(name = "invitation_status", nullable = false, length = 30)
    @Builder.Default
    private String invitationStatus = "ACTIVE";

    @Column(name = "invited_email")
    private String invitedEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
