package org.meetingschedulingplatform.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateSlotDto(@NotNull Instant startTime,
                            @NotNull Instant endTime,
                            @Min(1) @Max(1440) Integer slotDurationMinutes) {
}