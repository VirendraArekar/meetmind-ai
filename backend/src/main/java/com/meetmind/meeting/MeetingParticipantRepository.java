package com.meetmind.meeting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {
    List<MeetingParticipant> findByMeetingId(UUID meetingId);
    void deleteByMeetingId(UUID meetingId);
}
