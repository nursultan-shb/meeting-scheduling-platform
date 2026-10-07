package org.meetingschedulingplatform.dtos;

import org.meetingschedulingplatform.domain.entities.User;

import java.util.UUID;

public record UserDto(UUID id, String email, String name) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getName());
    }
}