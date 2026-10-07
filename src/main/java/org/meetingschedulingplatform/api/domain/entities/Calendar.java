package org.meetingschedulingplatform.api.domain.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.meetingschedulingplatform.enums.TimeSlotStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "Calendars")
public class Calendar extends BaseEntity {
    @Id
    @GeneratedValue(generator = "use-or-generate")
    UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    User user;

    public static Calendar of(User user) {
        Calendar calendar = new Calendar();
        calendar.setUser(user);
        return calendar;
    }

    public List<TimeSlot> openSlots(Instant start, Instant end, Duration slotLength) {
        if (!start.isBefore(end) || slotLength.isZero() || slotLength.isNegative()) {
            throw new IllegalArgumentException("Invalid slot range or length");
        }

        List<TimeSlot> slots = new ArrayList<>();
        for (Instant slotStart = start; slotStart.isBefore(end); slotStart = slotStart.plus(slotLength)) {
            Instant slotEnd = slotStart.plus(slotLength);
            if (slotEnd.isAfter(end)) {
                throw new IllegalArgumentException("Range is not a multiple of the slot length");
            }
            TimeSlot slot = new TimeSlot();
            slot.setCalendar(this);
            slot.setStartTime(slotStart);
            slot.setEndTime(slotEnd);
            slot.setStatus(TimeSlotStatus.FREE);
            slots.add(slot);
        }
        return slots;
    }
}

