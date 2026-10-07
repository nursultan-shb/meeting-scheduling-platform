package org.meetingschedulingplatform.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.meetingschedulingplatform.enums.TimeSlotStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "Time_Slots")
public class TimeSlot extends BaseEntity {
    @Id
    @GeneratedValue(generator = "use-or-generate")
    UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    Calendar calendar;

    @Column(name = "start_time")
    Instant startTime;
    @Column(name = "end_time")
    Instant endTime;

    @Enumerated(EnumType.STRING)
    TimeSlotStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    Meeting meeting;

    public boolean isBusy() {
        return this.getStatus() == TimeSlotStatus.BUSY;
    }

    public boolean isBooked() {
        return this.getMeeting() != null;
    }

    public TimeInterval interval() {
        return new TimeInterval(startTime, endTime);
    }
}
