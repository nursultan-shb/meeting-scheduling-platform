package org.meetingschedulingplatform.dtos;

import org.meetingschedulingplatform.domain.entities.TimeSlot;
import org.meetingschedulingplatform.enums.TimeSlotStatus;

import java.time.Instant;
import java.util.UUID;

public record SlotDto(UUID id, Instant startTime, Instant endTime, TimeSlotStatus status) {
    public static SlotDto from(TimeSlot slot) {
        return new SlotDto(slot.getId(), slot.getStartTime(), slot.getEndTime(), slot.getStatus());
    }
}
