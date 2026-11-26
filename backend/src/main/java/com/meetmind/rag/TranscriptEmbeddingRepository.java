package com.meetmind.rag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TranscriptEmbeddingRepository extends JpaRepository<TranscriptEmbedding, UUID> {
    List<TranscriptEmbedding> findByMeetingIdOrderByChunkIndexAsc(UUID meetingId);
    List<TranscriptEmbedding> findByOrganizationId(UUID organizationId);
    void deleteByMeetingId(UUID meetingId);
}
