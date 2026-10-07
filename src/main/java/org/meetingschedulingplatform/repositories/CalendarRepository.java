package org.meetingschedulingplatform.repositories;

import jakarta.persistence.LockModeType;
import org.meetingschedulingplatform.domain.entities.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, UUID> {
    Optional<Calendar> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Calendar c WHERE c.user.id = :userId")
    Optional<Calendar> findByUserIdForUpdate(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Calendar c WHERE c.user.id IN :userIds ORDER BY c.id")
    List<Calendar> findAllByUserIdsForUpdate(Collection<UUID> userIds);
}