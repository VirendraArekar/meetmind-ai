package com.meetmind.transcription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    Optional<Transcript> findByMeetingId(UUID meetingId);
}
