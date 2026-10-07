package org.meetingschedulingplatform.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.meetingschedulingplatform.dtos.CreateUserDto;
import org.meetingschedulingplatform.dtos.UserDto;
import org.meetingschedulingplatform.services.UserService;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserRestController {
    private final UserService userService;

    @PostMapping
    public HttpEntity<?> createUser(@Valid @RequestBody CreateUserDto dto) {
        UserDto user = userService.createUser(dto);
        var httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/users/" + user.id()).build().toUri());
        return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable UUID userId) {
        return userService.getUser(userId);
    }
}
