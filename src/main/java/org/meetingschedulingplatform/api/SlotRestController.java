package org.meetingschedulingplatform.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.meetingschedulingplatform.dtos.CreateSlotDto;
import org.meetingschedulingplatform.dtos.SlotDto;
import org.meetingschedulingplatform.dtos.UpdateSlotDto;
import org.meetingschedulingplatform.dtos.UpdateSlotStatusDto;
import org.meetingschedulingplatform.entities.PageDto;
import org.meetingschedulingplatform.enums.TimeSlotStatus;
import org.meetingschedulingplatform.services.SlotService;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}/slots")
@RequiredArgsConstructor
public class SlotRestController {
    private final SlotService slotService;

    @PostMapping
    public ResponseEntity<List<SlotDto>> createSlots(@PathVariable UUID userId, @Valid @RequestBody CreateSlotDto dto) {
        List<SlotDto> slots = slotService.createSlots(userId, dto).stream().map(SlotDto::from).toList();
        var response = ResponseEntity.status(HttpStatus.CREATED);
        return response.body(slots);
    }

    @GetMapping
    public PageDto<SlotDto> getSlots(
            @PathVariable UUID userId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) TimeSlotStatus status,
            @ParameterObject Pageable pageable
    ) {
        return PageDto.from(slotService.getSlots(userId, from, to, status, pageable), SlotDto::from);
    }

    @PutMapping("/{slotId}")
    public SlotDto updateSlot(@PathVariable UUID userId, @PathVariable UUID slotId,
                              @Valid @RequestBody UpdateSlotDto dto) {
        return SlotDto.from(slotService.updateSlot(userId, slotId, dto));
    }

    @PutMapping("/{slotId}/status")
    public SlotDto updateSlotStatus(@PathVariable UUID userId, @PathVariable UUID slotId,
                                    @Valid @RequestBody UpdateSlotStatusDto dto) {
        return SlotDto.from(slotService.updateSlotStatus(userId, slotId, dto.status()));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> deleteSlot(@PathVariable UUID userId, @PathVariable UUID slotId) {
        slotService.deleteSlot(userId, slotId);
        return ResponseEntity.noContent().build();
    }
}
