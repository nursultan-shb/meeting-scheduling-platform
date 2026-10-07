package org.meetingschedulingplatform.api.exceptions;

import java.util.UUID;

public class SlotNotFoundException extends NotFoundException {
    public SlotNotFoundException(UUID uuid) {
        super("Could not find a time slot by id: " + uuid);
    }
}