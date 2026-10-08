package org.meetingschedulingplatform.api.exceptions;

import java.util.UUID;

public class MeetingNotFoundException extends NotFoundException {
    public MeetingNotFoundException(UUID uuid) {
        super("Could not find a meeting by id: " + uuid);
    }
}
