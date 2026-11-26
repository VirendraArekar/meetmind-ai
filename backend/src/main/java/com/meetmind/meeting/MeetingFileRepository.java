package com.meetmind.meeting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingFileRepository extends JpaRepository<MeetingFile, UUID> {
    List<MeetingFile> findByMeetingIdOrderByCreatedAtDesc(UUID meetingId);
    Optional<MeetingFile> findFirstByMeetingIdOrderByCreatedAtDesc(UUID meetingId);
}
