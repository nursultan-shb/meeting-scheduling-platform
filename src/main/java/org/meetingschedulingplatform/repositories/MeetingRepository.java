package org.meetingschedulingplatform.repositories;


import org.meetingschedulingplatform.domain.entities.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    @Query("SELECT m.timeSlot.calendar.user.id FROM Meeting m WHERE m.id = :meetingId")
    Optional<UUID> findOrganizerId(UUID meetingId);

    /**
     * Loads the meeting with its slot, organizer and participants in one query.
     */
    @Query("""
        SELECT DISTINCT m FROM Meeting m
        JOIN FETCH m.timeSlot s
        JOIN FETCH s.calendar c
        JOIN FETCH c.user
        LEFT JOIN FETCH m.participants p
        LEFT JOIN FETCH p.user
        WHERE m.id = :meetingId
    """)
    Optional<Meeting> findDetailedById(UUID meetingId);
}
