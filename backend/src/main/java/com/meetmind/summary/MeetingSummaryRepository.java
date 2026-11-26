package com.meetmind.summary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, UUID> {
    Optional<MeetingSummary> findFirstByMeetingIdOrderByUpdatedAtDesc(UUID meetingId);
}
