package org.meetingschedulingplatform.repositories;

import org.meetingschedulingplatform.domain.entities.TimeSlot;
import org.meetingschedulingplatform.enums.TimeSlotStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {
    Optional<TimeSlot> findByIdAndCalendarId(UUID id, UUID calendarId);

    @Query("SELECT s.calendar.user.id FROM TimeSlot s WHERE s.id = :slotId")
    Optional<UUID> findOwnerUserId(UUID slotId);

    @Query("""
        SELECT s FROM TimeSlot s
        WHERE s.calendar.id = :calendarId
        AND s.startTime < :to
        AND s.endTime > :from
    """)
    Page<TimeSlot> findPageInRange(UUID calendarId, Instant from, Instant to, Pageable pageable);

    @Query("""
        SELECT s FROM TimeSlot s
        WHERE s.calendar.id = :calendarId
        AND s.status = :status
        AND s.startTime < :to
        AND s.endTime > :from
    """)
    Page<TimeSlot> findPageInRangeWithStatus(UUID calendarId, Instant from, Instant to, TimeSlotStatus status,
                                             Pageable pageable);

    @Query("""
        SELECT COUNT(s) > 0 FROM TimeSlot s
        WHERE s.calendar.id = :calendarId
        AND s.startTime < :end
        AND s.endTime > :start
    """)
    boolean existsOverlappingSlot(UUID calendarId, Instant start, Instant end);

    @Query("""
        SELECT COUNT(s) > 0 FROM TimeSlot s
        WHERE s.calendar.id = :calendarId
        AND s.id <> :excludedSlotId
        AND s.startTime < :end
        AND s.endTime > :start
    """)
    boolean existsOverlappingSlotExcluding(UUID calendarId, UUID excludedSlotId, Instant start, Instant end);

    @Query("""
        SELECT DISTINCT s.calendar.user.id FROM TimeSlot s
        WHERE s.calendar.user.id IN :userIds
        AND s.status = org.meetingschedulingplatform.enums.TimeSlotStatus.BUSY
        AND s.id <> :excludedSlotId
        AND s.startTime < :end
        AND s.endTime > :start
    """)
    List<UUID> findUsersWithBusySlots(Collection<UUID> userIds, UUID excludedSlotId, Instant start, Instant end);
}
