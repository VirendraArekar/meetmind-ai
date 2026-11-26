package com.meetmind.config;

import com.meetmind.actionitem.ActionItem;
import com.meetmind.actionitem.ActionItemRepository;
import com.meetmind.ai.AiProviderRouter;
import com.meetmind.ai.AiUsageLog;
import com.meetmind.ai.AiUsageLogRepository;
import com.meetmind.meeting.Meeting;
import com.meetmind.meeting.MeetingParticipant;
import com.meetmind.meeting.MeetingParticipantRepository;
import com.meetmind.meeting.MeetingRepository;
import com.meetmind.organization.Organization;
import com.meetmind.organization.OrganizationMember;
import com.meetmind.organization.OrganizationMemberRepository;
import com.meetmind.organization.OrganizationRepository;
import com.meetmind.organization.Team;
import com.meetmind.organization.TeamRepository;
import com.meetmind.rag.TranscriptEmbedding;
import com.meetmind.rag.TranscriptEmbeddingRepository;
import com.meetmind.summary.MeetingDecision;
import com.meetmind.summary.MeetingDecisionRepository;
import com.meetmind.summary.MeetingSummary;
import com.meetmind.summary.MeetingSummaryRepository;
import com.meetmind.transcription.Transcript;
import com.meetmind.transcription.TranscriptRepository;
import com.meetmind.transcription.TranscriptSegment;
import com.meetmind.transcription.TranscriptSegmentRepository;
import com.meetmind.user.Role;
import com.meetmind.user.RoleRepository;
import com.meetmind.user.User;
import com.meetmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Seeds a demo workspace with 250 realistic meetings, transcripts, summaries,
 * action items, and analytics telemetry on first startup when enabled.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final MeetingSummaryRepository summaryRepository;
    private final MeetingDecisionRepository decisionRepository;
    private final ActionItemRepository actionItemRepository;
    private final TranscriptEmbeddingRepository embeddingRepository;
    private final AiUsageLogRepository aiUsageLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${app.seed.demo-data:true}")
    private boolean seedEnabled;

    private static final String[] CATEGORIES = {"ENGINEERING", "PRODUCT", "SALES", "STANDUP", "GENERAL"};
    private static final String[] TOPICS = {
            "Payment Gateway Migration", "Q4 Roadmap Alignment", "Customer Onboarding Funnel",
            "API Latency Optimization", "SOC2 Security Audit", "Mobile App Redesign",
            "Enterprise Deal Review", "Database Sharding Strategy", "AI Pipeline Evaluation",
            "Design System Tokens"
    };
    private static final String[] SPEAKERS = {"Alice Chen", "Bob Patel", "Maya Lin", "Liam Torres", "Sofia Kim"};
    private static final String[] PRIORITIES = {"HIGH", "MEDIUM", "LOW"};

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }
        String demoEmail = "demo@meetmind.ai";
        if (userRepository.existsByEmailIgnoreCase(demoEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

        User demoUser = userRepository.save(User.builder()
                .email(demoEmail)
                .passwordHash(passwordEncoder.encode("Demo@123456"))
                .fullName("Alex Rivera")
                .jobTitle("VP of Product")
                .emailVerified(true)
                .roles(Set.of(adminRole))
                .build());

        Organization org = organizationRepository.save(Organization.builder()
                .name("MeetMind Demo Workspace")
                .slug("meetmind-demo")
                .owner(demoUser)
                .build());

        memberRepository.save(OrganizationMember.builder()
                .organization(org)
                .user(demoUser)
                .workspaceRole("ADMIN")
                .invitationStatus("ACTIVE")
                .build());

        teamRepository.save(Team.builder().organization(org).name("Platform Engineering").description("Core API and AI infra").build());
        teamRepository.save(Team.builder().organization(org).name("Product & Design").description("UX and roadmap planning").build());

        log.info("Seeding 250 demo meetings for {}...", demoEmail);
        Instant now = Instant.now();

        for (int i = 1; i <= 250; i++) {
            String category = CATEGORIES[i % CATEGORIES.length];
            String topic = TOPICS[i % TOPICS.length];
            String speakerA = SPEAKERS[i % SPEAKERS.length];
            String speakerB = SPEAKERS[(i + 1) % SPEAKERS.length];
            int duration = 600 + (i % 6) * 300;
            Instant createdAt = now.minus(i % 30, ChronoUnit.DAYS).minus(i % 12, ChronoUnit.HOURS);

            Meeting meeting = meetingRepository.save(Meeting.builder()
                    .organizationId(org.getId())
                    .createdBy(demoUser.getId())
                    .title(topic + " #" + i)
                    .description("Team sync covering " + topic.toLowerCase() + ", milestones, and blockers.")
                    .category(category)
                    .visibility(i % 3 == 0 ? "SHARED" : "PRIVATE")
                    .status("SUMMARIZED")
                    .sourceType(i % 4 == 0 ? "LIVE_RECORDING" : "UPLOAD")
                    .durationSeconds(duration)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());

            participantRepository.save(MeetingParticipant.builder()
                    .meetingId(meeting.getId()).displayName(speakerA).role("HOST").build());
            participantRepository.save(MeetingParticipant.builder()
                    .meetingId(meeting.getId()).displayName(speakerB).role("PARTICIPANT").build());

            String seg1 = "Let's review the current status of " + topic + " and confirm our release targets.";
            String seg2 = "We finalized the architecture and need to complete QA verification by Friday.";

            Transcript transcript = transcriptRepository.save(Transcript.builder()
                    .meetingId(meeting.getId())
                    .language("en")
                    .provider("openai")
                    .fullText(speakerA + ": " + seg1 + "\n" + speakerB + ": " + seg2)
                    .status("COMPLETED")
                    .build());

            segmentRepository.save(TranscriptSegment.builder()
                    .transcriptId(transcript.getId()).speakerName(speakerA).segmentIndex(0)
                    .startSeconds(0.0).endSeconds(18.0).content(seg1).confidence(0.96).build());
            segmentRepository.save(TranscriptSegment.builder()
                    .transcriptId(transcript.getId()).speakerName(speakerB).segmentIndex(1)
                    .startSeconds(18.0).endSeconds(42.0).content(seg2).confidence(0.94).build());

            // Index first 40 meetings with embeddings for instant semantic search & RAG chat
            if (i <= 40) {
                String chunkText = "[00:00] " + speakerA + ": " + seg1 + "\n[00:18] " + speakerB + ": " + seg2;
                float[] vec = AiProviderRouter.deterministicEmbedding(chunkText, 1536);
                try {
                    embeddingRepository.save(TranscriptEmbedding.builder()
                            .meetingId(meeting.getId())
                            .organizationId(org.getId())
                            .chunkIndex(0)
                            .startSeconds(0.0)
                            .endSeconds(42.0)
                            .content(chunkText)
                            .embeddingJson(objectMapper.writeValueAsString(vec))
                            .build());
                } catch (Exception ignored) {
                }
            }

            summaryRepository.save(MeetingSummary.builder()
                    .meetingId(meeting.getId())
                    .summaryLength("DETAILED")
                    .provider("openai")
                    .executiveSummary("Aligned on " + topic + " milestones and assigned QA sign-off before release.")
                    .objectives("Review progress and unblock " + topic + ".")
                    .discussionPoints("- [00:00] " + speakerA + ": " + seg1 + "\n- [00:18] " + speakerB + ": " + seg2)
                    .highlights("Architecture finalized; QA window confirmed for Friday.")
                    .decisionsSummary("Approved rollout plan for " + topic + ".")
                    .problemsIdentified("Tight QA timeline before Friday freeze.")
                    .risksDependencies("Dependent on staging environment stability.")
                    .nextSteps("Complete QA checklist and update stakeholders.")
                    .followUpRequirements("Share MOM with engineering and product leads.")
                    .build());

            decisionRepository.save(MeetingDecision.builder()
                    .meetingId(meeting.getId())
                    .title("Approve " + topic + " rollout")
                    .description("Team agreed to proceed with staging verification.")
                    .decidedBy(speakerA)
                    .transcriptTimestamp("00:18")
                    .build());

            actionItemRepository.save(ActionItem.builder()
                    .meetingId(meeting.getId())
                    .organizationId(org.getId())
                    .title("Complete QA verification for " + topic)
                    .description("Validate end-to-end flows before release.")
                    .assignedToName(speakerB)
                    .priority(PRIORITIES[i % PRIORITIES.length])
                    .dueDate(LocalDate.now().plusDays((i % 7) + 1))
                    .status(i % 2 == 0 ? "COMPLETED" : "PENDING")
                    .transcriptTimestamp("00:18")
                    .build());

            aiUsageLogRepository.save(AiUsageLog.builder()
                    .organizationId(org.getId())
                    .meetingId(meeting.getId())
                    .userId(demoUser.getId())
                    .operation(i % 2 == 0 ? "TRANSCRIPTION" : "SUMMARIZATION")
                    .provider("openai")
                    .model(i % 2 == 0 ? "whisper-1" : "gpt-4o-mini")
                    .tokensUsed(320 + (i % 5) * 80)
                    .latencyMs(450)
                    .success(true)
                    .build());
        }

        log.info("Finished seeding 250 demo meetings.");
    }
}
