package com.meetmind.transcription;

import com.meetmind.ai.AiUsageService;
import com.meetmind.ai.TranscriptionProvider;
import com.meetmind.common.exception.BadRequestException;
import com.meetmind.common.exception.ResourceNotFoundException;
import com.meetmind.meeting.Meeting;
import com.meetmind.meeting.MeetingFile;
import com.meetmind.meeting.MeetingFileRepository;
import com.meetmind.meeting.MeetingRepository;
import com.meetmind.meeting.MeetingService;
import com.meetmind.rag.RagIndexingService;
import com.meetmind.storage.StorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

    public record SegmentDto(UUID id, String speakerName, int segmentIndex, double startSeconds, double endSeconds, String content, double confidence) {}
    public record TranscriptDto(UUID id, UUID meetingId, String language, String provider, String status, String fullText, List<SegmentDto> segments, Instant updatedAt) {}
    public record UpdateSegmentItem(UUID id, @NotBlank String speakerName, @NotBlank String content, double startSeconds, double endSeconds) {}
    public record UpdateTranscriptRequest(@NotEmpty List<@Valid UpdateSegmentItem> segments) {}

    private final MeetingService meetingService;
    private final MeetingRepository meetingRepository;
    private final MeetingFileRepository fileRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final StorageService storageService;
    private final TranscriptionProvider transcriptionProvider;
    private final RagIndexingService ragIndexingService;
    private final AiUsageService aiUsageService;

    @Transactional
    public TranscriptDto transcribeMeeting(UUID userId, UUID meetingId) {
        Meeting meeting = meetingService.requireAccessibleMeeting(userId, meetingId);
        MeetingFile file = fileRepository.findFirstByMeetingIdOrderByCreatedAtDesc(meetingId)
                .orElseThrow(() -> new BadRequestException("Upload an audio or video recording before transcribing"));

        meeting.setStatus("TRANSCRIBING");
        meetingRepository.save(meeting);

        return executeTranscription(userId, meeting, file);
    }

    @Async("aiTaskExecutor")
    public void transcribeMeetingAsync(UUID userId, UUID meetingId) {
        try {
            transcribeMeeting(userId, meetingId);
        } catch (Exception ex) {
            log.error("Async transcription failed for meeting {}", meetingId, ex);
            meetingRepository.findByIdAndDeletedAtIsNull(meetingId).ifPresent(m -> {
                m.setStatus("FAILED");
                meetingRepository.save(m);
            });
        }
    }

    @Transactional
    protected TranscriptDto executeTranscription(UUID userId, Meeting meeting, MeetingFile file) {
        long start = System.currentTimeMillis();
        byte[] bytes = storageService.load(file.getStorageKey(), file.getStorageProvider());
        TranscriptionProvider.TranscriptionResult result = transcriptionProvider.transcribe(
                file.getOriginalFilename(), file.getContentType(), bytes, meeting.getTitle());

        Transcript transcript = transcriptRepository.findByMeetingId(meeting.getId())
                .orElse(Transcript.builder().meetingId(meeting.getId()).build());
        transcript.setLanguage(result.language());
        transcript.setProvider(result.provider());
        transcript.setFullText(result.fullText());
        transcript.setStatus("COMPLETED");
        transcript.setErrorMessage(null);
        transcript = transcriptRepository.save(transcript);

        segmentRepository.deleteByTranscriptId(transcript.getId());
        List<TranscriptSegment> savedSegments = new ArrayList<>();
        List<RagIndexingService.ChunkInput> ragChunks = new ArrayList<>();

        int idx = 0;
        for (TranscriptionProvider.TranscriptSegmentResult seg : result.segments()) {
            TranscriptSegment saved = segmentRepository.save(TranscriptSegment.builder()
                    .transcriptId(transcript.getId())
                    .speakerName(seg.speakerName())
                    .segmentIndex(idx++)
                    .startSeconds(seg.startSeconds())
                    .endSeconds(seg.endSeconds())
                    .content(seg.content())
                    .confidence(seg.confidence())
                    .build());
            savedSegments.add(saved);
            ragChunks.add(new RagIndexingService.ChunkInput(
                    seg.startSeconds(),
                    seg.endSeconds(),
                    "[" + formatTime(seg.startSeconds()) + "] " + seg.speakerName() + ": " + seg.content()
            ));
        }

        ragIndexingService.indexTranscriptChunks(meeting.getOrganizationId(), meeting.getId(), userId, ragChunks);

        meeting.setStatus("TRANSCRIBED");
        meetingRepository.save(meeting);

        aiUsageService.record(
                meeting.getOrganizationId(),
                meeting.getId(),
                userId,
                "TRANSCRIPTION",
                result.provider(),
                result.model(),
                Math.max(50, result.fullText().length() / 4),
                System.currentTimeMillis() - start,
                true
        );

        return toDto(transcript, savedSegments);
    }

    @Transactional(readOnly = true)
    public TranscriptDto getTranscript(UUID userId, UUID meetingId) {
        meetingService.requireAccessibleMeeting(userId, meetingId);
        Transcript transcript = transcriptRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript not found for this meeting"));
        List<TranscriptSegment> segments = segmentRepository.findByTranscriptIdOrderBySegmentIndexAsc(transcript.getId());
        return toDto(transcript, segments);
    }

    @Transactional
    public TranscriptDto updateTranscript(UUID userId, UUID meetingId, UpdateTranscriptRequest req) {
        Meeting meeting = meetingService.requireAccessibleMeeting(userId, meetingId);
        Transcript transcript = transcriptRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript not found for this meeting"));

        segmentRepository.deleteByTranscriptId(transcript.getId());
        List<TranscriptSegment> updatedSegments = new ArrayList<>();
        List<RagIndexingService.ChunkInput> ragChunks = new ArrayList<>();
        StringBuilder fullTextBuilder = new StringBuilder();

        int idx = 0;
        for (UpdateSegmentItem item : req.segments()) {
            TranscriptSegment saved = segmentRepository.save(TranscriptSegment.builder()
                    .transcriptId(transcript.getId())
                    .speakerName(item.speakerName().trim())
                    .segmentIndex(idx++)
                    .startSeconds(item.startSeconds())
                    .endSeconds(item.endSeconds())
                    .content(item.content().trim())
                    .confidence(1.0)
                    .build());
            updatedSegments.add(saved);
            if (!fullTextBuilder.isEmpty()) {
                fullTextBuilder.append("\n");
            }
            fullTextBuilder.append(saved.getSpeakerName()).append(": ").append(saved.getContent());
            ragChunks.add(new RagIndexingService.ChunkInput(
                    saved.getStartSeconds(),
                    saved.getEndSeconds(),
                    "[" + formatTime(saved.getStartSeconds()) + "] " + saved.getSpeakerName() + ": " + saved.getContent()
            ));
        }

        transcript.setFullText(fullTextBuilder.toString());
        transcript = transcriptRepository.save(transcript);
        ragIndexingService.indexTranscriptChunks(meeting.getOrganizationId(), meeting.getId(), userId, ragChunks);

        return toDto(transcript, updatedSegments);
    }

    public static String formatTime(double seconds) {
        int total = (int) Math.max(0, Math.round(seconds));
        int mins = total / 60;
        int secs = total % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private TranscriptDto toDto(Transcript t, List<TranscriptSegment> segments) {
        return new TranscriptDto(
                t.getId(),
                t.getMeetingId(),
                t.getLanguage(),
                t.getProvider(),
                t.getStatus(),
                t.getFullText(),
                segments.stream()
                        .map(s -> new SegmentDto(s.getId(), s.getSpeakerName(), s.getSegmentIndex(),
                                s.getStartSeconds(), s.getEndSeconds(), s.getContent(), s.getConfidence()))
                        .toList(),
                t.getUpdatedAt()
        );
    }
}
