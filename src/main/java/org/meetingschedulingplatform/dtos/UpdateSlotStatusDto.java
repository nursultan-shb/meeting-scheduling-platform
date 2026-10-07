package org.meetingschedulingplatform.dtos;


import jakarta.validation.constraints.NotNull;
import org.meetingschedulingplatform.enums.TimeSlotStatus;

public record UpdateSlotStatusDto(@NotNull TimeSlotStatus status) {
}
