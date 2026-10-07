package org.meetingschedulingplatform.services;

import org.meetingschedulingplatform.api.exceptions.DtoValidationException;

import java.time.Duration;
import java.time.Instant;

public class TimeRanges {
    static final Duration MAX_QUERY_RANGE = Duration.ofDays(366);

    private TimeRanges() {
    }

    public static void validateSlotRange(Instant startTime, Instant endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new DtoValidationException("Start time must be before end time");
        }
    }

    public static void validateQueryRange(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new DtoValidationException("'from' must be before 'to'");
        }
        if (Duration.between(from, to).compareTo(MAX_QUERY_RANGE) > 0) {
            throw new DtoValidationException("The queried range must not exceed " + MAX_QUERY_RANGE.toDays() + " days");
        }
    }
}
