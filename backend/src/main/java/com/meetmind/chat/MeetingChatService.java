package com.meetmind.chat;

import com.meetmind.ai.AiUsageService;
import com.meetmind.ai.LlmProvider;
import com.meetmind.meeting.Meeting;
import com.meetmind.meeting.MeetingService;
import com.meetmind.rag.RagIndexingService;
import com.meetmind.transcription.TranscriptionService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingChatService {

    public record ChatRequest(@NotBlank String question) {}
    public record CitationDto(String timestamp, String excerpt, double score) {}
    public record ChatMessageDto(UUID id, String role, String content, List<CitationDto> citations, Instant createdAt) {}
    public record ChatResponseDto(UUID sessionId, ChatMessageDto answer, List<ChatMessageDto> history) {}

    private final MeetingService meetingService;
    private final RagIndexingService ragIndexingService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final LlmProvider llmProvider;
    private final AiUsageService aiUsageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatResponseDto ask(UUID userId, UUID meetingId, ChatRequest req) {
        Meeting meeting = meetingService.requireAccessibleMeeting(userId, meetingId);
        ChatSession session = sessionRepository.findFirstByMeetingIdAndUserIdOrderByCreatedAtDesc(meetingId, userId)
                .orElseGet(() -> sessionRepository.save(ChatSession.builder()
                        .meetingId(meetingId)
                        .userId(userId)
                        .title(meeting.getTitle() + " Q&A")
                        .build()));

        messageRepository.save(ChatMessage.builder()
                .sessionId(session.getId())
                .role("USER")
                .content(req.question().trim())
                .build());

        long start = System.currentTimeMillis();
        List<RagIndexingService.ScoredChunk> retrieved = ragIndexingService.searchMeeting(meetingId, req.question(), 4);

        List<CitationDto> citations = retrieved.stream()
                .map(sc -> new CitationDto(
                        TranscriptionService.formatTime(sc.chunk().getStartSeconds()) + " - " +
                                TranscriptionService.formatTime(sc.chunk().getEndSeconds()),
                        sc.chunk().getContent(),
                        Math.round(sc.score() * 1000.0) / 1000.0
                ))
                .toList();

        String answerText;
        String providerName = "rag-grounding";
        String modelName = "none";
        int tokensUsed = 0;

        if (retrieved.isEmpty()) {
            answerText = "No transcript data is available for this meeting yet. Please transcribe the meeting first so I can answer based on verified transcript context.";
        } else {
            StringBuilder context = new StringBuilder();
            for (CitationDto c : citations) {
                context.append("[").append(c.timestamp()).append("]\n").append(c.excerpt()).append("\n\n");
            }
            String systemPrompt = """
                    You are MeetMind AI Assistant. Answer the user's question ONLY using the provided meeting transcript excerpts.
                    Always cite relevant transcript timestamps like [MM:SS].
                    If the answer cannot be found in the excerpts, explicitly state: "That information is not mentioned in the meeting transcript."
                    Never invent or extrapolate meeting details.
                    """;
            String userPrompt = "Transcript Excerpts:\n" + context + "\nQuestion: " + req.question();
            LlmProvider.LlmResult result = llmProvider.complete(systemPrompt, userPrompt);
            providerName = result.provider();
            modelName = result.model();
            tokensUsed = result.tokensUsed();

            if ("heuristic-local".equals(result.provider())) {
                CitationDto top = citations.getFirst();
                answerText = "Based on the meeting transcript at [" + top.timestamp() + "]:\n" + top.excerpt();
            } else {
                answerText = result.content();
            }
        }

        ChatMessage assistantMsg = messageRepository.save(ChatMessage.builder()
                .sessionId(session.getId())
                .role("ASSISTANT")
                .content(answerText)
                .citationsJson(serializeCitations(citations))
                .build());

        aiUsageService.record(
                meeting.getOrganizationId(),
                meetingId,
                userId,
                "RAG_CHAT",
                providerName,
                modelName,
                tokensUsed,
                System.currentTimeMillis() - start,
                true
        );

        List<ChatMessageDto> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(this::toDto)
                .toList();

        return new ChatResponseDto(session.getId(), toDto(assistantMsg), history);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getHistory(UUID userId, UUID meetingId) {
        meetingService.requireAccessibleMeeting(userId, meetingId);
        return sessionRepository.findFirstByMeetingIdAndUserIdOrderByCreatedAtDesc(meetingId, userId)
                .map(session -> messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                        .map(this::toDto)
                        .toList())
                .orElse(List.of());
    }

    private String serializeCitations(List<CitationDto> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<CitationDto> deserializeCitations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private ChatMessageDto toDto(ChatMessage m) {
        return new ChatMessageDto(m.getId(), m.getRole(), m.getContent(), deserializeCitations(m.getCitationsJson()), m.getCreatedAt());
    }
}
