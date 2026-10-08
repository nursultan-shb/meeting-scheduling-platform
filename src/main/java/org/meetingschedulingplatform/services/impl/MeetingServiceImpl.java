package org.meetingschedulingplatform.services.impl;


import lombok.RequiredArgsConstructor;

import org.meetingschedulingplatform.api.exceptions.ConflictException;
import org.meetingschedulingplatform.api.exceptions.MeetingNotFoundException;
import org.meetingschedulingplatform.api.exceptions.SlotNotFoundException;
import org.meetingschedulingplatform.api.exceptions.UserNotFoundException;
import org.meetingschedulingplatform.domain.entities.Calendar;
import org.meetingschedulingplatform.domain.entities.Meeting;
import org.meetingschedulingplatform.domain.entities.TimeSlot;
import org.meetingschedulingplatform.dtos.BookMeetingDto;
import org.meetingschedulingplatform.dtos.MeetingDto;
import org.meetingschedulingplatform.dtos.UpdateMeetingDto;
import org.meetingschedulingplatform.enums.TimeSlotStatus;
import org.meetingschedulingplatform.repositories.CalendarRepository;
import org.meetingschedulingplatform.repositories.MeetingParticipantRepository;
import org.meetingschedulingplatform.repositories.MeetingRepository;
import org.meetingschedulingplatform.repositories.TimeSlotRepository;
import org.meetingschedulingplatform.services.MeetingService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MeetingServiceImpl implements MeetingService {
    private final MeetingRepository meetingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CalendarRepository calendarRepository;
    private final MeetingParticipantRepository participantRepository;

    @Transactional
    @Override
    public UUID bookMeeting(BookMeetingDto dto) {
        Set<UUID> participantIds = dto.participants() == null ? Set.of() : new LinkedHashSet<>(dto.participants());
        Set<UUID> attendeeIds = getMeetingAttendees(dto, participantIds);
        Map<UUID, Calendar> calendarsByUserId = lockCalendars(attendeeIds);

        TimeSlot timeSlot = timeSlotRepository.findById(dto.slotId())
                .orElseThrow(() -> new SlotNotFoundException(dto.slotId()));

        validateTimeSlotsForBooking(timeSlot, dto, attendeeIds);

        Meeting meeting = createNewMeeting(dto, timeSlot);
        participantIds.forEach(id -> meeting.addParticipant(calendarsByUserId.get(id).getUser()));
        Meeting saved = meetingRepository.save(meeting);

        timeSlot.setStatus(TimeSlotStatus.BUSY);
        timeSlot.setMeeting(saved);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    @Override
    public MeetingDto getMeeting(UUID meetingId) {
        return MeetingDto.from(meetingRepository.findDetailedById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId)));
    }

    @Transactional
    @Override
    public MeetingDto updateMeeting(UUID meetingId, UpdateMeetingDto dto) {
        UUID organizerId = getOrganizerIdByMeetingId(meetingId);
        Set<UUID> participantIds = new LinkedHashSet<>(dto.participants());
        Map<UUID, Calendar> calendarsByUserId = lockCalendars(withOrganizer(participantIds, organizerId));

        Meeting meeting = meetingRepository.findDetailedById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        Set<UUID> currentIds = meeting.getParticipants().stream()
                .map(participant -> participant.getUser().getId())
                .collect(Collectors.toSet());
        Set<UUID> addedIds = new LinkedHashSet<>(participantIds);
        addedIds.removeAll(currentIds);

        if (!addedIds.isEmpty()) {
            Set<UUID> unavailable = findUnavailableAttendees(addedIds, meeting.getTimeSlot());
            if (!unavailable.isEmpty()) {
                throw new ConflictException("Users are not available at this time: " + unavailable);
            }
        }

        meeting.setTitle(dto.title());
        meeting.setDescription(dto.description());
        meeting.getParticipants().removeIf(participant -> !participantIds.contains(participant.getUser().getId()));
        addedIds.forEach(id -> meeting.addParticipant(calendarsByUserId.get(id).getUser()));

        return MeetingDto.from(meeting);
    }

    @Transactional
    @Override
    public void cancelMeeting(UUID meetingId) {
        UUID organizerId = getOrganizerIdByMeetingId(meetingId);
        lockCalendars(Set.of(organizerId));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        TimeSlot timeSlot = meeting.getTimeSlot();
        timeSlot.setMeeting(null);
        timeSlot.setStatus(TimeSlotStatus.FREE);
        meetingRepository.delete(meeting);
    }

    private Map<UUID, Calendar> lockCalendars(Set<UUID> userIds) {
        Map<UUID, Calendar> calendarsByUserId = calendarRepository.findAllByUserIdsForUpdate(userIds).stream()
                .collect(Collectors.toMap(calendar -> calendar.getUser().getId(), Function.identity()));
        userIds.stream()
                .filter(id -> !calendarsByUserId.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new UserNotFoundException(id);
                });
        return calendarsByUserId;
    }

    private static Set<UUID> withOrganizer(Set<UUID> participantIds, UUID organizerId) {
        Set<UUID> attendeeIds = new HashSet<>(participantIds);
        attendeeIds.add(organizerId);
        return attendeeIds;
    }

    private Set<UUID> findUnavailableAttendees(Set<UUID> attendeeIds, TimeSlot timeSlot) {
        Set<UUID> unavailable = new TreeSet<>(timeSlotRepository.findUsersWithBusySlots(
                attendeeIds, timeSlot.getId(), timeSlot.getStartTime(), timeSlot.getEndTime()));
        unavailable.addAll(participantRepository.findUsersInMeetings(
                attendeeIds, timeSlot.getStartTime(), timeSlot.getEndTime()));
        return unavailable;
    }

    private void validateTimeSlotsForBooking(TimeSlot timeSlot, BookMeetingDto dto, Set<UUID> attendeeIds) {
        if (timeSlot.isBusy()) {
            throw new ConflictException("Time slot " + dto.slotId() + " is not free");
        }
        Set<UUID> unavailable = findUnavailableAttendees(attendeeIds, timeSlot);
        if (!unavailable.isEmpty()) {
            throw new ConflictException("Users are not available at this time: " + unavailable);
        }
    }

    private UUID getOrganizerIdByMeetingId(UUID meetingId) {
         return meetingRepository.findOrganizerId(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
    }

    private Meeting createNewMeeting(BookMeetingDto dto, TimeSlot timeSlot) {
        Meeting meeting = new Meeting();
        meeting.setTitle(dto.title());
        meeting.setDescription(dto.description());
        meeting.setTimeSlot(timeSlot);
        return meeting;
    }

    private Set<UUID> getMeetingAttendees(BookMeetingDto dto, Set<UUID> participantIds) {
        UUID ownerId = timeSlotRepository.findOwnerUserId(dto.slotId())
                .orElseThrow(() -> new SlotNotFoundException(dto.slotId()));
        return withOrganizer(participantIds, ownerId);
    }
}
