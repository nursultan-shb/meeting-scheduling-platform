package org.meetingschedulingplatform.services;

import org.meetingschedulingplatform.dtos.CreateSlotDto;
import org.meetingschedulingplatform.dtos.UpdateSlotDto;
import org.meetingschedulingplatform.api.domain.entities.TimeSlot;
import org.meetingschedulingplatform.enums.TimeSlotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SlotService {
    List<TimeSlot> createSlots(UUID userId, CreateSlotDto dto);

    Page<TimeSlot> getSlots(UUID userId, Instant from, Instant to, TimeSlotStatus status, Pageable pageable);

    TimeSlot updateSlot(UUID userId, UUID slotId, UpdateSlotDto dto);

    TimeSlot updateSlotStatus(UUID userId, UUID slotId, TimeSlotStatus status);

    void deleteSlot(UUID userId, UUID slotId);
}
