package org.meetingschedulingplatform.api.exceptions;

public class DtoValidationException extends RuntimeException {
    public DtoValidationException(String message) {
        super(message);
    }
}

