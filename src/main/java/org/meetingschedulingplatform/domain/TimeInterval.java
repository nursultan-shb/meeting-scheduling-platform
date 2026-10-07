package org.meetingschedulingplatform.domain;

import java.time.Instant;
import java.util.Optional;

public record TimeInterval(Instant start, Instant end) {
    public TimeInterval {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Interval start must be before its end");
        }
    }

    public Optional<TimeInterval> clipTo(TimeInterval window) {
        Instant clippedStart = start.isAfter(window.start) ? start : window.start;
        Instant clippedEnd = end.isBefore(window.end) ? end : window.end;
        return clippedStart.isBefore(clippedEnd)
                ? Optional.of(new TimeInterval(clippedStart, clippedEnd))
                : Optional.empty();
    }
}