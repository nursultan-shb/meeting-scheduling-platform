package org.meetingschedulingplatform.services;

import org.meetingschedulingplatform.dtos.BookMeetingDto;
import org.meetingschedulingplatform.dtos.MeetingDto;
import org.meetingschedulingplatform.dtos.UpdateMeetingDto;

import java.util.UUID;

public interface MeetingService {
    UUID bookMeeting(BookMeetingDto dto);

    MeetingDto getMeeting(UUID meetingId);

    MeetingDto updateMeeting(UUID meetingId, UpdateMeetingDto dto);

    void cancelMeeting(UUID meetingId);
}