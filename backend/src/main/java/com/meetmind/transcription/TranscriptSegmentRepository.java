package com.meetmind.transcription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, UUID> {
    List<TranscriptSegment> findByTranscriptIdOrderBySegmentIndexAsc(UUID transcriptId);
    void deleteByTranscriptId(UUID transcriptId);
}
