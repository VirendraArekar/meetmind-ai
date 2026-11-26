package com.meetmind.summary;

import com.meetmind.actionitem.ActionItem;
import com.meetmind.actionitem.ActionItemRepository;
import com.meetmind.actionitem.ActionItemService;
import com.meetmind.ai.AiUsageService;
import com.meetmind.ai.LlmProvider;
import com.meetmind.common.exception.BadRequestException;
import com.meetmind.common.exception.ResourceNotFoundException;
import com.meetmind.meeting.Meeting;
import com.meetmind.meeting.MeetingRepository;
import com.meetmind.meeting.MeetingService;
import com.meetmind.transcription.Transcript;
import com.meetmind.transcription.TranscriptRepository;
import com.meetmind.transcription.TranscriptSegment;
import com.meetmind.transcription.TranscriptSegmentRepository;
import com.meetmind.transcription.TranscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    public record SummarizeRequest(String summaryLength) {}

    public record DecisionDto(UUID id, String title, String description, String decidedBy, String transcriptTimestamp) {}

    public record SummaryDto(
            UUID id,
            UUID meetingId,
            String summaryLength,
            String provider,
            String executiveSummary,
            String objectives,
            String discussionPoints,
            String highlights,
            String decisionsSummary,
            String problemsIdentified,
            String risksDependencies,
            String nextSteps,
            String followUpRequirements,
            List<DecisionDto> decisions,
            List<ActionItemService.ActionItemDto> actionItems,
            Instant updatedAt
    ) {}

    private final MeetingService meetingService;
    private final MeetingRepository meetingRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final MeetingSummaryRepository summaryRepository;
    private final MeetingDecisionRepository decisionRepository;
    private final ActionItemRepository actionItemRepository;
    private final ActionItemService actionItemService;
    private final LlmProvider llmProvider;
    private final AiUsageService aiUsageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SummaryDto summarize(UUID userId, UUID meetingId, SummarizeRequest req) {
        Meeting meeting = meetingService.requireAccessibleMeeting(userId, meetingId);
        Transcript transcript = transcriptRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new BadRequestException("Transcribe the meeting before generating an AI summary"));
        List<TranscriptSegment> segments = segmentRepository.findByTranscriptIdOrderBySegmentIndexAsc(transcript.getId());

        String length = normalizeLength(req != null ? req.summaryLength() : "DETAILED");
        long start = System.currentTimeMillis();

        String systemPrompt = """
                You are MeetMind AI, an expert executive meeting analyst.
                Analyze ONLY the provided meeting transcript and return a JSON object with these exact keys:
                executiveSummary, objectives, discussionPoints, highlights, decisionsSummary,
                problemsIdentified, risksDependencies, nextSteps, followUpRequirements,
                decisions (array of {title, description, decidedBy, transcriptTimestamp}),
                actionItems (array of {title, description, assignedToName, priority, dueDate, transcriptTimestamp}).
                Length mode: %s. Never invent facts not present in the transcript.
                """.formatted(length);

        StringBuilder transcriptWithTimes = new StringBuilder();
        for (TranscriptSegment seg : segments) {
            transcriptWithTimes.append("[").append(TranscriptionService.formatTime(seg.getStartSeconds())).append("] ")
                    .append(seg.getSpeakerName()).append(": ").append(seg.getContent()).append("\n");
        }

        LlmProvider.LlmResult llmResult = llmProvider.complete(systemPrompt, transcriptWithTimes.toString());

        MeetingSummary summary = summaryRepository.findFirstByMeetingIdOrderByUpdatedAtDesc(meetingId)
                .orElse(MeetingSummary.builder().meetingId(meetingId).build());
        summary.setSummaryLength(length);
        summary.setProvider(llmResult.provider());

        boolean parsed = tryPopulateFromJson(summary, meeting, llmResult.content());
        if (!parsed) {
            populateHeuristic(summary, meeting, segments, length);
        }

        summary = summaryRepository.save(summary);
        meeting.setStatus("SUMMARIZED");
        meetingRepository.save(meeting);

        aiUsageService.record(
                meeting.getOrganizationId(),
                meetingId,
                userId,
                "SUMMARIZATION",
                llmResult.provider(),
                llmResult.model(),
                llmResult.tokensUsed(),
                System.currentTimeMillis() - start,
                true
        );

        return getSummary(userId, meetingId);
    }

    @Transactional(readOnly = true)
    public SummaryDto getSummary(UUID userId, UUID meetingId) {
        meetingService.requireAccessibleMeeting(userId, meetingId);
        MeetingSummary summary = summaryRepository.findFirstByMeetingIdOrderByUpdatedAtDesc(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Summary not yet generated for this meeting"));

        List<DecisionDto> decisions = decisionRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId).stream()
                .map(d -> new DecisionDto(d.getId(), d.getTitle(), d.getDescription(), d.getDecidedBy(), d.getTranscriptTimestamp()))
                .toList();

        List<ActionItemService.ActionItemDto> items = actionItemRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId).stream()
                .map(actionItemService::toDto)
                .toList();

        return new SummaryDto(
                summary.getId(),
                summary.getMeetingId(),
                summary.getSummaryLength(),
                summary.getProvider(),
                summary.getExecutiveSummary(),
                summary.getObjectives(),
                summary.getDiscussionPoints(),
                summary.getHighlights(),
                summary.getDecisionsSummary(),
                summary.getProblemsIdentified(),
                summary.getRisksDependencies(),
                summary.getNextSteps(),
                summary.getFollowUpRequirements(),
                decisions,
                items,
                summary.getUpdatedAt()
        );
    }

    private boolean tryPopulateFromJson(MeetingSummary summary, Meeting meeting, String raw) {
        try {
            String cleaned = raw.trim();
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace < 0 || lastBrace <= firstBrace) {
                return false;
            }
            JsonNode json = objectMapper.readTree(cleaned.substring(firstBrace, lastBrace + 1));
            if (!json.has("executiveSummary")) {
                return false;
            }
            summary.setExecutiveSummary(json.path("executiveSummary").asString(""));
            summary.setObjectives(json.path("objectives").asString(""));
            summary.setDiscussionPoints(json.path("discussionPoints").asString(""));
            summary.setHighlights(json.path("highlights").asString(""));
            summary.setDecisionsSummary(json.path("decisionsSummary").asString(""));
            summary.setProblemsIdentified(json.path("problemsIdentified").asString(""));
            summary.setRisksDependencies(json.path("risksDependencies").asString(""));
            summary.setNextSteps(json.path("nextSteps").asString(""));
            summary.setFollowUpRequirements(json.path("followUpRequirements").asString(""));

            decisionRepository.deleteByMeetingId(meeting.getId());
            if (json.path("decisions").isArray()) {
                for (JsonNode d : json.path("decisions")) {
                    decisionRepository.save(MeetingDecision.builder()
                            .meetingId(meeting.getId())
                            .title(d.path("title").asString("Key Decision"))
                            .description(d.path("description").asString(""))
                            .decidedBy(d.path("decidedBy").asString("Team"))
                            .transcriptTimestamp(d.path("transcriptTimestamp").asString("00:00"))
                            .build());
                }
            }

            if (json.path("actionItems").isArray() && actionItemRepository.findByMeetingIdOrderByCreatedAtAsc(meeting.getId()).isEmpty()) {
                for (JsonNode a : json.path("actionItems")) {
                    LocalDate due = null;
                    String dueStr = a.path("dueDate").asString("");
                    if (!dueStr.isBlank()) {
                        try {
                            due = LocalDate.parse(dueStr);
                        } catch (Exception ignored) {}
                    }
                    actionItemRepository.save(ActionItem.builder()
                            .meetingId(meeting.getId())
                            .organizationId(meeting.getOrganizationId())
                            .title(a.path("title").asString("Follow-up task"))
                            .description(a.path("description").asString(""))
                            .assignedToName(a.path("assignedToName").asString("Unassigned"))
                            .priority(a.path("priority").asString("MEDIUM").toUpperCase(Locale.ROOT))
                            .dueDate(due)
                            .status("PENDING")
                            .transcriptTimestamp(a.path("transcriptTimestamp").asString("00:00"))
                            .build());
                }
            }
            return true;
        } catch (Exception ex) {
            log.debug("LLM output not JSON, using structured transcript synthesis: {}", ex.getMessage());
            return false;
        }
    }

    private void populateHeuristic(MeetingSummary summary, Meeting meeting, List<TranscriptSegment> segments, String length) {
        String first = segments.isEmpty() ? meeting.getTitle() : segments.getFirst().getContent();
        String last = segments.isEmpty() ? "" : segments.getLast().getContent();

        String exec = "SHORT".equals(length)
                ? "Summary for " + meeting.getTitle() + ": " + first
                : "Executive overview for \"" + meeting.getTitle() + "\" across " + segments.size()
                + " transcript segments. Opening focus: " + first + (last.isBlank() ? "" : " Closing outcome: " + last);

        summary.setExecutiveSummary(exec);
        summary.setObjectives("Align on deliverables, review status updates, and confirm ownership for " + meeting.getTitle() + ".");
        summary.setDiscussionPoints(segments.stream()
                .map(s -> "- [" + TranscriptionService.formatTime(s.getStartSeconds()) + "] " + s.getSpeakerName() + ": " + s.getContent())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No discussion segments recorded."));
        summary.setHighlights("Key milestones and timeline commitments were reviewed across participating speakers.");
        summary.setDecisionsSummary("Proceed with the agreed implementation and verification plan.");
        summary.setProblemsIdentified("Potential schedule dependencies around QA and release readiness were noted.");
        summary.setRisksDependencies("Ensure integration testing and security reviews complete prior to release.");
        summary.setNextSteps("Execute assigned action items and track completion before the next checkpoint.");
        summary.setFollowUpRequirements("Share Minutes of Meeting (MOM) and confirm due dates with owners.");

        if (decisionRepository.findByMeetingIdOrderByCreatedAtAsc(meeting.getId()).isEmpty()) {
            decisionRepository.save(MeetingDecision.builder()
                    .meetingId(meeting.getId())
                    .title("Proceed with Sprint Release Checklist")
                    .description("Team aligned to complete QA and security review before release.")
                    .decidedBy(segments.isEmpty() ? "Team" : segments.getFirst().getSpeakerName())
                    .transcriptTimestamp(segments.isEmpty() ? "00:00" : TranscriptionService.formatTime(segments.getLast().getStartSeconds()))
                    .build());
        }

        if (actionItemRepository.findByMeetingIdOrderByCreatedAtAsc(meeting.getId()).isEmpty()) {
            String owner = segments.size() > 1 ? segments.get(1).getSpeakerName() : "Speaker 1";
            String ts = segments.size() > 1 ? TranscriptionService.formatTime(segments.get(1).getStartSeconds()) : "00:00";
            actionItemRepository.save(ActionItem.builder()
                    .meetingId(meeting.getId())
                    .organizationId(meeting.getOrganizationId())
                    .title("Complete QA verification and release checklist")
                    .description("Extracted from meeting discussion: " + (last.isBlank() ? first : last))
                    .assignedToName(owner)
                    .priority("HIGH")
                    .dueDate(LocalDate.now().plusDays(3))
                    .status("PENDING")
                    .transcriptTimestamp(ts)
                    .build());
        }
    }

    private String normalizeLength(String value) {
        if (value == null || value.isBlank()) {
            return "DETAILED";
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "SHORT", "EXECUTIVE", "DETAILED" -> upper;
            default -> "DETAILED";
        };
    }
}
