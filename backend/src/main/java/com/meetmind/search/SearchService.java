package com.meetmind.search;

import com.meetmind.meeting.Meeting;
import com.meetmind.meeting.MeetingRepository;
import com.meetmind.organization.OrganizationService;
import com.meetmind.rag.RagIndexingService;
import com.meetmind.transcription.TranscriptRepository;
import com.meetmind.transcription.TranscriptionService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    public record SearchHitDto(UUID meetingId, String meetingTitle, String category, String snippet, String timestamp, double score) {}
    public record SemanticSearchRequest(@NotBlank String query, Integer limit) {}

    private final MeetingRepository meetingRepository;
    private final TranscriptRepository transcriptRepository;
    private final OrganizationService organizationService;
    private final RagIndexingService ragIndexingService;

    @Transactional(readOnly = true)
    public List<SearchHitDto> fullTextSearch(UUID userId, String query, String category) {
        UUID orgId = organizationService.requireDefaultOrganizationId(userId);
        List<Meeting> meetings = meetingRepository.findAllAccessibleByOrgAndUser(orgId, userId);
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<SearchHitDto> hits = new ArrayList<>();
        for (Meeting m : meetings) {
            if (category != null && !category.isBlank() && !m.getCategory().equalsIgnoreCase(category)) {
                continue;
            }
            boolean titleMatch = q.isEmpty() || m.getTitle().toLowerCase(Locale.ROOT).contains(q)
                    || (m.getDescription() != null && m.getDescription().toLowerCase(Locale.ROOT).contains(q));

            String transcriptText = transcriptRepository.findByMeetingId(m.getId())
                    .map(t -> t.getFullText())
                    .orElse("");
            boolean transcriptMatch = !q.isEmpty() && transcriptText.toLowerCase(Locale.ROOT).contains(q);

            if (titleMatch || transcriptMatch) {
                String snippet = transcriptMatch
                        ? extractSnippet(transcriptText, q)
                        : (m.getDescription() != null && !m.getDescription().isBlank() ? m.getDescription() : m.getTitle());
                hits.add(new SearchHitDto(m.getId(), m.getTitle(), m.getCategory(), snippet, null, titleMatch ? 1.0 : 0.8));
            }
        }
        return hits;
    }

    @Transactional(readOnly = true)
    public List<SearchHitDto> semanticSearch(UUID userId, SemanticSearchRequest req) {
        UUID orgId = organizationService.requireDefaultOrganizationId(userId);
        List<Meeting> accessible = meetingRepository.findAllAccessibleByOrgAndUser(orgId, userId);
        Map<UUID, Meeting> byId = new HashMap<>();
        for (Meeting m : accessible) {
            byId.put(m.getId(), m);
        }

        int topK = req.limit() != null && req.limit() > 0 ? Math.min(req.limit(), 20) : 8;
        return ragIndexingService.searchOrganization(orgId, req.query(), topK * 2).stream()
                .filter(sc -> byId.containsKey(sc.chunk().getMeetingId()))
                .limit(topK)
                .map(sc -> {
                    Meeting m = byId.get(sc.chunk().getMeetingId());
                    return new SearchHitDto(
                            m.getId(),
                            m.getTitle(),
                            m.getCategory(),
                            sc.chunk().getContent(),
                            TranscriptionService.formatTime(sc.chunk().getStartSeconds()),
                            Math.round(sc.score() * 1000.0) / 1000.0
                    );
                })
                .toList();
    }

    private String extractSnippet(String text, String q) {
        int idx = text.toLowerCase(Locale.ROOT).indexOf(q);
        if (idx < 0) {
            return text.substring(0, Math.min(180, text.length()));
        }
        int start = Math.max(0, idx - 60);
        int end = Math.min(text.length(), idx + 120);
        return text.substring(start, end);
    }
}
