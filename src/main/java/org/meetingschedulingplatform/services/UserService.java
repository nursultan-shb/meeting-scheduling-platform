package org.meetingschedulingplatform.services;

import org.meetingschedulingplatform.dtos.CreateUserDto;
import org.meetingschedulingplatform.dtos.UserDto;

import java.util.UUID;

public interface UserService {
    UserDto createUser(CreateUserDto dto);

    UserDto getUser(UUID userId);
}
