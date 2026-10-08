package org.meetingschedulingplatform.services;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.meetingschedulingplatform.api.exceptions.ConflictException;
import org.meetingschedulingplatform.api.exceptions.DtoValidationException;
import org.meetingschedulingplatform.api.exceptions.SlotNotFoundException;
import org.meetingschedulingplatform.api.exceptions.UserNotFoundException;
import org.meetingschedulingplatform.domain.entities.Calendar;
import org.meetingschedulingplatform.domain.entities.Meeting;
import org.meetingschedulingplatform.domain.entities.TimeSlot;
import org.meetingschedulingplatform.domain.entities.User;
import org.meetingschedulingplatform.dtos.CreateSlotDto;
import org.meetingschedulingplatform.dtos.UpdateSlotDto;
import org.meetingschedulingplatform.enums.TimeSlotStatus;
import org.meetingschedulingplatform.repositories.CalendarRepository;
import org.meetingschedulingplatform.repositories.TimeSlotRepository;
import org.meetingschedulingplatform.services.impl.SlotServiceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlotServiceTest {
    private static final Instant START = Instant.parse("2026-01-01T09:00:00Z");
    private static final Instant END = START.plus(1, ChronoUnit.HOURS);

    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private CalendarRepository calendarRepository;

    private final UUID userId = UUID.randomUUID();
    private Calendar calendar;
    private SlotServiceImpl testee;

    @BeforeEach
    public void setUp() {
        testee = new SlotServiceImpl(timeSlotRepository, calendarRepository);
        User user = new User();
        user.setId(userId);
        calendar = Calendar.of(user);
        calendar.setId(UUID.randomUUID());
    }

    @Test
    public void createSlotTest() {
        CreateSlotDto dto = new CreateSlotDto(START, END, null);

        lockCalendar();
        when(timeSlotRepository.existsOverlappingSlot(calendar.getId(), START, END)).thenReturn(false);
        when(timeSlotRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<TimeSlot> result = testee.createSlots(userId, dto);

        assertEquals(1, result.size());
        assertEquals(calendar, result.get(0).getCalendar());
        assertEquals(START, result.get(0).getStartTime());
        assertEquals(END, result.get(0).getEndTime());
        assertEquals(TimeSlotStatus.FREE, result.get(0).getStatus());
    }

    @Test
    public void createSlotsOfConfiguredDurationTest() {
        Instant end = START.plus(Duration.ofHours(8));
        CreateSlotDto dto = new CreateSlotDto(START, end, 30);

        lockCalendar();
        when(timeSlotRepository.existsOverlappingSlot(calendar.getId(), START, end)).thenReturn(false);
        when(timeSlotRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<TimeSlot> result = testee.createSlots(userId, dto);

        assertEquals(16, result.size());
        assertEquals(START, result.get(0).getStartTime());
        assertEquals(end, result.get(15).getEndTime());
    }

    @Test
    public void createSlotsWithRangeNotMultipleOfDurationTest() {
        lockCalendar();

        assertThrows(DtoValidationException.class,
                () -> testee.createSlots(userId, new CreateSlotDto(START, START.plus(Duration.ofMinutes(50)), 30)));
        verify(timeSlotRepository, never()).saveAll(any());
    }

    @Test
    public void createTooManySlotsTest() {
        lockCalendar();

        assertThrows(DtoValidationException.class,
                () -> testee.createSlots(userId, new CreateSlotDto(START, START.plus(Duration.ofDays(1)), 1)));
        verify(timeSlotRepository, never()).saveAll(any());
    }

    @Test
    public void createSlotForUnknownUserTest() {
        when(calendarRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> testee.createSlots(userId, new CreateSlotDto(START, END, null)));
    }

    @Test
    public void createZeroLengthSlotTest() {
        lockCalendar();

        assertThrows(DtoValidationException.class, () -> testee.createSlots(userId, new CreateSlotDto(START, START, null)));
        verify(timeSlotRepository, never()).saveAll(any());
    }

    @Test
    public void createOverlappingSlotTest() {
        lockCalendar();
        when(timeSlotRepository.existsOverlappingSlot(calendar.getId(), START, END)).thenReturn(true);

        assertThrows(ConflictException.class, () -> testee.createSlots(userId, new CreateSlotDto(START, END, null)));
        verify(timeSlotRepository, never()).saveAll(any());
    }

    @Test
    public void getSlotsWithRangeTooLargeTest() {
        assertThrows(DtoValidationException.class,
                () -> testee.getSlots(userId, START, START.plus(Duration.ofDays(400)), null, PageRequest.of(0, 50)));
    }

    @Test
    public void getSlotsIsSortedByStartTimeTest() {
        TimeSlot slot = slot(TimeSlotStatus.FREE);
        Pageable expected = PageRequest.of(2, 10, Sort.by("startTime"));
        when(calendarRepository.findByUserId(userId)).thenReturn(Optional.of(calendar));
        when(timeSlotRepository.findPageInRange(calendar.getId(), START, END, expected))
                .thenReturn(new PageImpl<>(List.of(slot), expected, 21));

        Page<TimeSlot> result = testee.getSlots(userId, START, END, null, PageRequest.of(2, 10, Sort.by("status")));

        assertEquals(List.of(slot), result.getContent());
        assertEquals(21, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
    }

    @Test
    public void getSlotsFiltersByStatusTest() {
        TimeSlot slot = slot(TimeSlotStatus.BUSY);
        Pageable expected = PageRequest.of(0, 50, Sort.by("startTime"));
        when(calendarRepository.findByUserId(userId)).thenReturn(Optional.of(calendar));
        when(timeSlotRepository.findPageInRangeWithStatus(calendar.getId(), START, END, TimeSlotStatus.BUSY, expected))
                .thenReturn(new PageImpl<>(List.of(slot), expected, 1));

        assertEquals(List.of(slot),
                testee.getSlots(userId, START, END, TimeSlotStatus.BUSY, PageRequest.of(0, 50)).getContent());
    }

    @Test
    public void deleteSlotTest() {
        TimeSlot slot = ownedSlot(TimeSlotStatus.FREE);

        testee.deleteSlot(userId, slot.getId());

        verify(timeSlotRepository).delete(slot);
    }

    @Test
    public void deleteSlotOfAnotherUserTest() {
        UUID slotId = UUID.randomUUID();
        lockCalendar();
        when(timeSlotRepository.findByIdAndCalendarId(slotId, calendar.getId())).thenReturn(Optional.empty());

        assertThrows(SlotNotFoundException.class, () -> testee.deleteSlot(userId, slotId));
        verify(timeSlotRepository, never()).delete(any());
    }

    @Test
    public void deleteBookedSlotTest() {
        TimeSlot slot = ownedSlot(TimeSlotStatus.BUSY);
        slot.setMeeting(new Meeting());

        assertThrows(ConflictException.class, () -> testee.deleteSlot(userId, slot.getId()));
        verify(timeSlotRepository, never()).delete(any());
    }

    @Test
    public void updateSlotTest() {
        TimeSlot slot = ownedSlot(TimeSlotStatus.FREE);
        UpdateSlotDto dto = new UpdateSlotDto(START.plus(2, ChronoUnit.HOURS), END.plus(2, ChronoUnit.HOURS));
        when(timeSlotRepository.existsOverlappingSlotExcluding(calendar.getId(), slot.getId(), dto.startTime(), dto.endTime()))
                .thenReturn(false);

        TimeSlot result = testee.updateSlot(userId, slot.getId(), dto);

        assertEquals(dto.startTime(), result.getStartTime());
        assertEquals(dto.endTime(), result.getEndTime());
    }

    @Test
    public void updateSlotToOverlappingTimeTest() {
        TimeSlot slot = ownedSlot(TimeSlotStatus.FREE);
        UpdateSlotDto dto = new UpdateSlotDto(START.plus(2, ChronoUnit.HOURS), END.plus(2, ChronoUnit.HOURS));
        when(timeSlotRepository.existsOverlappingSlotExcluding(calendar.getId(), slot.getId(), dto.startTime(), dto.endTime()))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> testee.updateSlot(userId, slot.getId(), dto));
        assertEquals(START, slot.getStartTime());
    }

    @Test
    public void updateSlotWithInvalidRangeTest() {
        TimeSlot slot = ownedSlot(TimeSlotStatus.FREE);

        assertThrows(DtoValidationException.class,
                () -> testee.updateSlot(userId, slot.getId(), new UpdateSlotDto(END, START)));
    }

    @Test
    public void updateBookedSlotTest() {
        TimeSlot slot = ownedSlot(TimeSlotStatus.BUSY);
        slot.setMeeting(new Meeting());

        assertThrows(ConflictException.class,
                () -> testee.updateSlot(userId, slot.getId(), new UpdateSlotDto(START, END)));
    }

    @Test
    public void markSlotBusyAndFreeTest() {
        TimeSlot slot = ownedSlot(TimeSlotStatus.FREE);

        assertEquals(TimeSlotStatus.BUSY, testee.updateSlotStatus(userId, slot.getId(), TimeSlotStatus.BUSY).getStatus());
        assertEquals(TimeSlotStatus.FREE, testee.updateSlotStatus(userId, slot.getId(), TimeSlotStatus.FREE).getStatus());
    }

    @Test
    public void markBookedSlotFreeTest() {
        TimeSlot slot = ownedSlot(TimeSlotStatus.BUSY);
        slot.setMeeting(new Meeting());

        assertThrows(ConflictException.class,
                () -> testee.updateSlotStatus(userId, slot.getId(), TimeSlotStatus.FREE));
        assertEquals(TimeSlotStatus.BUSY, slot.getStatus());
    }

    private void lockCalendar() {
        when(calendarRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(calendar));
    }

    private TimeSlot ownedSlot(TimeSlotStatus status) {
        TimeSlot slot = slot(status);
        lockCalendar();
        when(timeSlotRepository.findByIdAndCalendarId(slot.getId(), calendar.getId())).thenReturn(Optional.of(slot));
        return slot;
    }

    private TimeSlot slot(TimeSlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.setId(UUID.randomUUID());
        slot.setCalendar(calendar);
        slot.setStartTime(START);
        slot.setEndTime(END);
        slot.setStatus(status);
        return slot;
    }
}
