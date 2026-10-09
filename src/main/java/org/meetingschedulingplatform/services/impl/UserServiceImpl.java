package org.meetingschedulingplatform.services.impl;

import lombok.RequiredArgsConstructor;

import org.meetingschedulingplatform.api.exceptions.ConflictException;
import org.meetingschedulingplatform.api.exceptions.UserNotFoundException;
import org.meetingschedulingplatform.domain.entities.Calendar;
import org.meetingschedulingplatform.dtos.CreateUserDto;
import org.meetingschedulingplatform.dtos.UserDto;
import org.meetingschedulingplatform.domain.entities.User;
import org.meetingschedulingplatform.repositories.CalendarRepository;
import org.meetingschedulingplatform.repositories.UserRepository;
import org.meetingschedulingplatform.services.UserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CalendarRepository calendarRepository;

    @Override
    @Transactional
    public UserDto createUser(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ConflictException("User with email " + dto.email() + " already exists");
        }

        User user = new User();
        user.setEmail(dto.email());
        user.setName(dto.name());

        user = userRepository.save(user);
        calendarRepository.save(Calendar.of(user));
        return UserDto.from(user);
    }

    @Override
    public UserDto getUser(UUID userId) {
        return userRepository.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
