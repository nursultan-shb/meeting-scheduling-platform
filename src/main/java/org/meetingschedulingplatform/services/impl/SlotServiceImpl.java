package org.meetingschedulingplatform.services.impl;

import lombok.RequiredArgsConstructor;
import org.meetingschedulingplatform.api.exceptions.ConflictException;
import org.meetingschedulingplatform.api.exceptions.DtoValidationException;
import org.meetingschedulingplatform.api.exceptions.SlotNotFoundException;
import org.meetingschedulingplatform.api.exceptions.UserNotFoundException;
import org.meetingschedulingplatform.dtos.CreateSlotDto;
import org.meetingschedulingplatform.dtos.UpdateSlotDto;
import org.meetingschedulingplatform.entities.Calendar;
import org.meetingschedulingplatform.entities.TimeSlot;
import org.meetingschedulingplatform.enums.TimeSlotStatus;
import org.meetingschedulingplatform.repositories.CalendarRepository;
import org.meetingschedulingplatform.repositories.TimeSlotRepository;
import org.meetingschedulingplatform.services.SlotService;
import org.meetingschedulingplatform.services.TimeRanges;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class SlotServiceImpl implements SlotService {
    static final int MAX_SLOTS_PER_REQUEST = 500;
    private static final Sort BY_START_TIME = Sort.by("startTime");

    private final TimeSlotRepository timeSlotRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    @Override
    public List<TimeSlot> createSlots(UUID userId, CreateSlotDto dto) {
        Calendar calendar = lockCalendar(userId);
        TimeRanges.validateSlotRange(dto.startTime(), dto.endTime());

        Duration range = Duration.between(dto.startTime(), dto.endTime());
        Duration slotLength = dto.slotDurationMinutes() == null ? range : Duration.ofMinutes(dto.slotDurationMinutes());
        long slotCount = range.dividedBy(slotLength);
        if (!slotLength.multipliedBy(slotCount).equals(range)) {
            throw new DtoValidationException("The time range must be a multiple of the slot duration");
        }
        if (slotCount > MAX_SLOTS_PER_REQUEST) {
            throw new DtoValidationException("At most " + MAX_SLOTS_PER_REQUEST + " slots can be created at once");
        }
        // The new slots cover the whole range, so one check over the range covers all of them
        if (timeSlotRepository.existsOverlappingSlot(calendar.getId(), dto.startTime(), dto.endTime())) {
            throw new ConflictException("Time slot overlaps with an existing slot");
        }

        List<TimeSlot> slots = timeSlotRepository.saveAll(
                calendar.openSlots(dto.startTime(), dto.endTime(), slotLength));
        return slots;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TimeSlot> getSlots(UUID userId, Instant from, Instant to, TimeSlotStatus status, Pageable pageable) {
        TimeRanges.validateQueryRange(from, to);
        UUID calendarId = calendarRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId))
                .getId();
        Pageable byStartTime = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), BY_START_TIME);
        return status == null
                ? timeSlotRepository.findPageInRange(calendarId, from, to, byStartTime)
                : timeSlotRepository.findPageInRangeWithStatus(calendarId, from, to, status, byStartTime);
    }

    @Transactional
    @Override
    public TimeSlot updateSlot(UUID userId, UUID slotId, UpdateSlotDto dto) {
        Calendar calendar = lockCalendar(userId);
        TimeSlot slot = getSlot(calendar, slotId);
        if (slot.isBooked()) {
            throw new ConflictException("Time slot " + slotId + " is booked for a meeting and cannot be modified");
        }
        TimeRanges.validateSlotRange(dto.startTime(), dto.endTime());
        if (timeSlotRepository.existsOverlappingSlotExcluding(calendar.getId(), slotId, dto.startTime(), dto.endTime())) {
            throw new ConflictException("Time slot overlaps with an existing slot");
        }

        slot.setStartTime(dto.startTime());
        slot.setEndTime(dto.endTime());
        return slot;
    }

    @Transactional
    @Override
    public TimeSlot updateSlotStatus(UUID userId, UUID slotId, TimeSlotStatus status) {
        TimeSlot slot = getSlot(lockCalendar(userId), slotId);
        if (status == TimeSlotStatus.FREE && slot.isBooked()) {
            throw new ConflictException("Time slot " + slotId + " is booked for a meeting and cannot be freed");
        }

        slot.setStatus(status);
        return slot;
    }

    @Transactional
    @Override
    public void deleteSlot(UUID userId, UUID slotId) {
        TimeSlot slot = getSlot(lockCalendar(userId), slotId);
        if (slot.isBooked()) {
            throw new ConflictException("Time slot " + slotId + " is booked for a meeting and cannot be deleted");
        }
        timeSlotRepository.delete(slot);
    }

    private Calendar lockCalendar(UUID userId) {
        return calendarRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private TimeSlot getSlot(Calendar calendar, UUID slotId) {
        return timeSlotRepository.findByIdAndCalendarId(slotId, calendar.getId())
                .orElseThrow(() -> new SlotNotFoundException(slotId));
    }
}