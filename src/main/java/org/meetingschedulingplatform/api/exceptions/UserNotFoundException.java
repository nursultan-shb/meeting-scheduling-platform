package org.meetingschedulingplatform.api.exceptions;

import java.util.UUID;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(UUID uuid) {
        super("Could not find a user by id: " + uuid);
    }
}
