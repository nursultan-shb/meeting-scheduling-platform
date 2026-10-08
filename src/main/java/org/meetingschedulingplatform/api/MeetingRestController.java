package org.meetingschedulingplatform.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.meetingschedulingplatform.dtos.BookMeetingDto;
import org.meetingschedulingplatform.dtos.MeetingDto;
import org.meetingschedulingplatform.dtos.UpdateMeetingDto;
import org.meetingschedulingplatform.services.MeetingService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingRestController {
    private final MeetingService meetingService;

    @PostMapping
    public HttpEntity<?> bookMeeting(@Valid @RequestBody BookMeetingDto dto) {
        UUID meetingId = meetingService.bookMeeting(dto);

        var httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/meetings/" + meetingId.toString()).build().toUri());
        return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
    }

    @GetMapping("/{meetingId}")
    public MeetingDto getMeeting(@PathVariable UUID meetingId) {
        return meetingService.getMeeting(meetingId);
    }

    @PutMapping("/{meetingId}")
    public MeetingDto updateMeeting(@PathVariable UUID meetingId, @Valid @RequestBody UpdateMeetingDto dto) {
        return meetingService.updateMeeting(meetingId, dto);
    }

    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> cancelMeeting(@PathVariable UUID meetingId) {
        meetingService.cancelMeeting(meetingId);
        return ResponseEntity.noContent().build();
    }
}
