package com.meetmind.meeting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    @Query("""
            SELECT m FROM Meeting m
            WHERE m.organizationId = :orgId
              AND m.deletedAt IS NULL
              AND (m.visibility = 'SHARED' OR m.createdBy = :userId)
              AND (:category IS NULL OR m.category = :category)
              AND (:archived IS NULL OR m.archived = :archived)
            ORDER BY m.createdAt DESC
            """)
    Page<Meeting> findAccessibleMeetings(
            @Param("orgId") UUID orgId,
            @Param("userId") UUID userId,
            @Param("category") String category,
            @Param("archived") Boolean archived,
            Pageable pageable
    );

    Optional<Meeting> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            SELECT m FROM Meeting m
            WHERE m.organizationId = :orgId
              AND m.deletedAt IS NULL
              AND (m.visibility = 'SHARED' OR m.createdBy = :userId)
            ORDER BY m.createdAt DESC
            """)
    List<Meeting> findAllAccessibleByOrgAndUser(@Param("orgId") UUID orgId, @Param("userId") UUID userId);
}
