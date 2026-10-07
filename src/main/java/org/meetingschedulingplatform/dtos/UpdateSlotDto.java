package org.meetingschedulingplatform.dtos;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateSlotDto(@NotNull Instant startTime, @NotNull Instant endTime) {
}
