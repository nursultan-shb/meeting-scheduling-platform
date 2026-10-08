package org.meetingschedulingplatform.services;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.meetingschedulingplatform.api.exceptions.ConflictException;
import org.meetingschedulingplatform.api.exceptions.MeetingNotFoundException;
import org.meetingschedulingplatform.api.exceptions.SlotNotFoundException;
import org.meetingschedulingplatform.api.exceptions.UserNotFoundException;
import org.meetingschedulingplatform.domain.entities.Calendar;
import org.meetingschedulingplatform.domain.entities.Meeting;
import org.meetingschedulingplatform.domain.entities.TimeSlot;
import org.meetingschedulingplatform.domain.entities.User;
import org.meetingschedulingplatform.dtos.BookMeetingDto;
import org.meetingschedulingplatform.dtos.MeetingDto;
import org.meetingschedulingplatform.dtos.UpdateMeetingDto;
import org.meetingschedulingplatform.dtos.UserDto;
import org.meetingschedulingplatform.enums.TimeSlotStatus;
import org.meetingschedulingplatform.repositories.CalendarRepository;
import org.meetingschedulingplatform.repositories.MeetingParticipantRepository;
import org.meetingschedulingplatform.repositories.MeetingRepository;
import org.meetingschedulingplatform.repositories.TimeSlotRepository;
import org.meetingschedulingplatform.services.impl.MeetingServiceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MeetingServiceTest {
    private static final Instant START = Instant.parse("2026-01-01T09:00:00Z");
    private static final Instant END = START.plus(1, ChronoUnit.HOURS);

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private CalendarRepository calendarRepository;
    @Mock
    private MeetingParticipantRepository participantRepository;

    private MeetingServiceImpl testee;

    private Calendar ownerCalendar;
    private Calendar participantCalendar;
    private TimeSlot timeSlot;

    @BeforeEach
    public void setUp() {
        testee = new MeetingServiceImpl(meetingRepository, timeSlotRepository, calendarRepository,
                participantRepository);
        ownerCalendar = calendarOf(UUID.randomUUID());
        participantCalendar = calendarOf(UUID.randomUUID());

        timeSlot = new TimeSlot();
        timeSlot.setId(UUID.randomUUID());
        timeSlot.setCalendar(ownerCalendar);
        timeSlot.setStartTime(START);
        timeSlot.setEndTime(END);
        timeSlot.setStatus(TimeSlotStatus.FREE);
    }

    @Test
    public void bookMeetingTest() {
        UUID participantId = participantCalendar.getUser().getId();
        BookMeetingDto dto = new BookMeetingDto("Meeting", "Sprint planning",
                timeSlot.getId(), List.of(participantId, participantId));
        Set<UUID> attendees = Set.of(ownerId(), participantId);

        givenSlotAndCalendars(attendees, List.of(ownerCalendar, participantCalendar));
        when(timeSlotRepository.findUsersWithBusySlots(attendees, timeSlot.getId(), START, END)).thenReturn(List.of());
        when(participantRepository.findUsersInMeetings(attendees, START, END)).thenReturn(List.of());
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        UUID resultId = testee.bookMeeting(dto);
        assertSame(resultId, timeSlot.getMeeting().getId());
    }

    @Test
    public void bookMeetingForUnknownSlotTest() {
        BookMeetingDto dto = new BookMeetingDto("Meeting", null, UUID.randomUUID(), List.of());

        when(timeSlotRepository.findOwnerUserId(dto.slotId())).thenReturn(Optional.empty());

        assertThrows(SlotNotFoundException.class, () -> testee.bookMeeting(dto));
    }

    @Test
    public void bookMeetingForBusySlotTest() {
        timeSlot.setStatus(TimeSlotStatus.BUSY);
        BookMeetingDto dto = new BookMeetingDto("Meeting", null, timeSlot.getId(), List.of());

        givenSlotAndCalendars(Set.of(ownerId()), List.of(ownerCalendar));

        assertThrows(ConflictException.class, () -> testee.bookMeeting(dto));
        verify(meetingRepository, never()).save(any());
    }

    @Test
    public void bookMeetingWithUnknownParticipantTest() {
        UUID unknownUserId = UUID.randomUUID();
        BookMeetingDto dto = new BookMeetingDto("Meeting", null, timeSlot.getId(), List.of(unknownUserId));

        when(timeSlotRepository.findOwnerUserId(timeSlot.getId())).thenReturn(Optional.of(ownerId()));
        when(calendarRepository.findAllByUserIdsForUpdate(Set.of(ownerId(), unknownUserId)))
                .thenReturn(List.of(ownerCalendar));

        UserNotFoundException e = assertThrows(UserNotFoundException.class, () -> testee.bookMeeting(dto));
        assertEquals("Could not find a user by id: " + unknownUserId, e.getMessage());
        assertEquals(TimeSlotStatus.FREE, timeSlot.getStatus());
        verify(meetingRepository, never()).save(any());
    }

    @Test
    public void bookMeetingWithBusyParticipantTest() {
        UUID participantId = participantCalendar.getUser().getId();
        BookMeetingDto dto = new BookMeetingDto("Meeting", null, timeSlot.getId(), List.of(participantId));
        Set<UUID> attendees = Set.of(ownerId(), participantId);

        givenSlotAndCalendars(attendees, List.of(ownerCalendar, participantCalendar));
        when(timeSlotRepository.findUsersWithBusySlots(attendees, timeSlot.getId(), START, END))
                .thenReturn(List.of(participantId));
        when(participantRepository.findUsersInMeetings(attendees, START, END)).thenReturn(List.of());

        ConflictException e = assertThrows(ConflictException.class, () -> testee.bookMeeting(dto));
        assertTrue(e.getMessage().contains(participantId.toString()));
        assertEquals(TimeSlotStatus.FREE, timeSlot.getStatus());
        verify(meetingRepository, never()).save(any());
    }

    @Test
    public void bookMeetingWhileOwnerAttendsAnotherMeetingTest() {
        BookMeetingDto dto = new BookMeetingDto("Meeting", null, timeSlot.getId(), List.of());
        Set<UUID> attendees = Set.of(ownerId());

        givenSlotAndCalendars(attendees, List.of(ownerCalendar));
        when(timeSlotRepository.findUsersWithBusySlots(attendees, timeSlot.getId(), START, END)).thenReturn(List.of());
        when(participantRepository.findUsersInMeetings(attendees, START, END)).thenReturn(List.of(ownerId()));

        assertThrows(ConflictException.class, () -> testee.bookMeeting(dto));
        verify(meetingRepository, never()).save(any());
    }

    @Test
    public void getMeetingTest() {
        Meeting meeting = bookedMeeting(participantCalendar.getUser());
        when(meetingRepository.findDetailedById(meeting.getId())).thenReturn(Optional.of(meeting));

        MeetingDto result = testee.getMeeting(meeting.getId());

        assertEquals(meeting.getTitle(), result.title());
        assertEquals(timeSlot.getId(), result.slotId());
        assertEquals(START, result.startTime());
        assertEquals(ownerId(), result.organizer().id());
        assertEquals(List.of(participantCalendar.getUser().getId()),
                result.participants().stream().map(UserDto::id).toList());
    }

    @Test
    public void getUnknownMeetingTest() {
        UUID meetingId = UUID.randomUUID();
        when(meetingRepository.findDetailedById(meetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> testee.getMeeting(meetingId));
    }

    @Test
    public void updateMeetingReplacesDetailsAndParticipantsTest() {
        Meeting meeting = bookedMeeting(participantCalendar.getUser());
        Calendar newParticipantCalendar = calendarOf(UUID.randomUUID());
        UUID newParticipantId = newParticipantCalendar.getUser().getId();
        UpdateMeetingDto dto = new UpdateMeetingDto("Renamed", "New description", List.of(newParticipantId));

        when(meetingRepository.findOrganizerId(meeting.getId())).thenReturn(Optional.of(ownerId()));
        when(calendarRepository.findAllByUserIdsForUpdate(Set.of(ownerId(), newParticipantId)))
                .thenReturn(List.of(ownerCalendar, newParticipantCalendar));
        when(meetingRepository.findDetailedById(meeting.getId())).thenReturn(Optional.of(meeting));
        when(timeSlotRepository.findUsersWithBusySlots(Set.of(newParticipantId), timeSlot.getId(), START, END))
                .thenReturn(List.of());
        when(participantRepository.findUsersInMeetings(Set.of(newParticipantId), START, END)).thenReturn(List.of());

        MeetingDto result = testee.updateMeeting(meeting.getId(), dto);

        assertEquals("Renamed", result.title());
        assertEquals("New description", result.description());
        assertEquals(List.of(newParticipantId), result.participants().stream().map(UserDto::id).toList());
    }

    @Test
    public void updateMeetingOnlyChecksAddedParticipantsTest() {
        Meeting meeting = bookedMeeting(participantCalendar.getUser());
        UUID participantId = participantCalendar.getUser().getId();
        UpdateMeetingDto dto = new UpdateMeetingDto("Renamed", null, List.of(participantId));

        when(meetingRepository.findOrganizerId(meeting.getId())).thenReturn(Optional.of(ownerId()));
        when(calendarRepository.findAllByUserIdsForUpdate(Set.of(ownerId(), participantId)))
                .thenReturn(List.of(ownerCalendar, participantCalendar));
        when(meetingRepository.findDetailedById(meeting.getId())).thenReturn(Optional.of(meeting));

        MeetingDto result = testee.updateMeeting(meeting.getId(), dto);

        assertEquals(List.of(participantId), result.participants().stream().map(UserDto::id).toList());
        verify(participantRepository, never()).findUsersInMeetings(any(), any(), any());
    }

    @Test
    public void updateMeetingWithBusyNewParticipantTest() {
        Meeting meeting = bookedMeeting();
        UUID participantId = participantCalendar.getUser().getId();
        UpdateMeetingDto dto = new UpdateMeetingDto("Renamed", null, List.of(participantId));

        when(meetingRepository.findOrganizerId(meeting.getId())).thenReturn(Optional.of(ownerId()));
        when(calendarRepository.findAllByUserIdsForUpdate(Set.of(ownerId(), participantId)))
                .thenReturn(List.of(ownerCalendar, participantCalendar));
        when(meetingRepository.findDetailedById(meeting.getId())).thenReturn(Optional.of(meeting));
        when(timeSlotRepository.findUsersWithBusySlots(Set.of(participantId), timeSlot.getId(), START, END))
                .thenReturn(List.of());
        when(participantRepository.findUsersInMeetings(Set.of(participantId), START, END))
                .thenReturn(List.of(participantId));

        assertThrows(ConflictException.class, () -> testee.updateMeeting(meeting.getId(), dto));
        assertEquals("Meeting", meeting.getTitle());
        assertEquals(0, meeting.getParticipants().size());
    }

    @Test
    public void updateUnknownMeetingTest() {
        UUID meetingId = UUID.randomUUID();
        when(meetingRepository.findOrganizerId(meetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class,
                () -> testee.updateMeeting(meetingId, new UpdateMeetingDto("x", null, List.of())));
    }

    @Test
    public void cancelMeetingFreesSlotTest() {
        Meeting meeting = bookedMeeting(participantCalendar.getUser());

        when(meetingRepository.findOrganizerId(meeting.getId())).thenReturn(Optional.of(ownerId()));
        when(calendarRepository.findAllByUserIdsForUpdate(Set.of(ownerId()))).thenReturn(List.of(ownerCalendar));
        when(meetingRepository.findById(meeting.getId())).thenReturn(Optional.of(meeting));

        testee.cancelMeeting(meeting.getId());

        assertEquals(TimeSlotStatus.FREE, timeSlot.getStatus());
        assertNull(timeSlot.getMeeting());
        verify(meetingRepository).delete(meeting);
    }

    @Test
    public void cancelUnknownMeetingTest() {
        UUID meetingId = UUID.randomUUID();
        when(meetingRepository.findOrganizerId(meetingId)).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> testee.cancelMeeting(meetingId));
        verify(meetingRepository, never()).delete(any());
    }

    private Meeting bookedMeeting(User... participants) {
        Meeting meeting = new Meeting();
        meeting.setId(UUID.randomUUID());
        meeting.setTitle("Meeting");
        meeting.setTimeSlot(timeSlot);
        for (User participant : participants) {
            meeting.addParticipant(participant);
        }
        timeSlot.setMeeting(meeting);
        timeSlot.setStatus(TimeSlotStatus.BUSY);
        return meeting;
    }

    private void givenSlotAndCalendars(Set<UUID> attendees, List<Calendar> calendars) {
        when(timeSlotRepository.findOwnerUserId(timeSlot.getId())).thenReturn(Optional.of(ownerId()));
        when(calendarRepository.findAllByUserIdsForUpdate(attendees)).thenReturn(calendars);
        when(timeSlotRepository.findById(timeSlot.getId())).thenReturn(Optional.of(timeSlot));
    }

    private UUID ownerId() {
        return ownerCalendar.getUser().getId();
    }

    private static Calendar calendarOf(UUID userId) {
        User user = new User();
        user.setId(userId);
        Calendar calendar = Calendar.of(user);
        calendar.setId(UUID.randomUUID());
        return calendar;
    }
}
