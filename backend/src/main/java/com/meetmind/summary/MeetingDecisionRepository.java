package com.meetmind.summary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingDecisionRepository extends JpaRepository<MeetingDecision, UUID> {
    List<MeetingDecision> findByMeetingIdOrderByCreatedAtAsc(UUID meetingId);
    void deleteByMeetingId(UUID meetingId);
}
