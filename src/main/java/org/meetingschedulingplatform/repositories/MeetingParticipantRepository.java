package org.meetingschedulingplatform.repositories;


import org.meetingschedulingplatform.domain.entities.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {
    /**
     * Returns those of the given users who participate in a meeting overlapping [start, end).
     */
    @Query("""
        SELECT DISTINCT p.user.id FROM MeetingParticipant p
        JOIN p.meeting m
        JOIN m.timeSlot s
        WHERE p.user.id IN :userIds
        AND s.startTime < :end
        AND s.endTime > :start
    """)
    List<UUID> findUsersInMeetings(Collection<UUID> userIds, Instant start, Instant end);
}
