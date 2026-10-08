package org.meetingschedulingplatform.dtos;

import org.meetingschedulingplatform.domain.entities.Meeting;
import org.meetingschedulingplatform.domain.entities.TimeSlot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeetingDto(UUID id,
                         String title,
                         String description,
                         UUID slotId,
                         Instant startTime,
                         Instant endTime,
                         UserDto organizer,
                         List<UserDto> participants) {
    public static MeetingDto from(Meeting meeting) {
        TimeSlot slot = meeting.getTimeSlot();
        return new MeetingDto(meeting.getId(), meeting.getTitle(), meeting.getDescription(),
                slot.getId(), slot.getStartTime(), slot.getEndTime(),
                UserDto.from(slot.getCalendar().getUser()),
                meeting.getParticipants().stream().map(participant -> UserDto.from(participant.getUser())).toList());
    }
}
