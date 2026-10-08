package org.meetingschedulingplatform.domain.entities;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "Meetings")
public class Meeting extends BaseEntity {
    @Id
    @GeneratedValue(generator = "use-or-generate")
    UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id")
    TimeSlot timeSlot;

    String title;
    String description;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MeetingParticipant> participants = new ArrayList<>();

    public void addParticipant(User user) {
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeeting(this);
        participant.setUser(user);

        participants.add(participant);
    }
}
