package org.meetingschedulingplatform.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateMeetingDto(@NotBlank @Size(max = 255) String title,
                               String description,
                               @NotNull List<@NotNull UUID> participants) {
}
