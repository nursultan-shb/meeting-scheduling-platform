package org.meetingschedulingplatform.services;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.meetingschedulingplatform.api.exceptions.ConflictException;
import org.meetingschedulingplatform.api.exceptions.UserNotFoundException;
import org.meetingschedulingplatform.domain.entities.Calendar;
import org.meetingschedulingplatform.domain.entities.User;
import org.meetingschedulingplatform.dtos.CreateUserDto;
import org.meetingschedulingplatform.dtos.UserDto;
import org.meetingschedulingplatform.repositories.CalendarRepository;
import org.meetingschedulingplatform.repositories.UserRepository;
import org.meetingschedulingplatform.services.impl.UserServiceImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private CalendarRepository calendarRepository;

    @InjectMocks
    private UserServiceImpl testee;

    @Test
    public void getUser() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("a@example.com");
        user.setName("A");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto result = testee.getUser(userId);
        assertEquals(new UserDto(userId, "a@example.com", "A"), result);
    }

    @Test
    public void testGetNotExistingUser() {
        UUID userId = UUID.randomUUID();

        Assertions.assertThrows(UserNotFoundException.class, () -> {
            testee.getUser(userId);
        });
    }

    @Test
    public void testCreateUserWithExistingEmail() {
        CreateUserDto dto = new CreateUserDto("a@example.com", "A");

        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        Assertions.assertThrows(ConflictException.class, () -> testee.createUser(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testCreateUserCreatesCalendar() {
        CreateUserDto dto = new CreateUserDto("a@example.com", "A");
        UUID generatedId = UUID.randomUUID();

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> {
            User user = i.getArgument(0);
            user.setId(generatedId);
            return user;
        });

        UserDto result = testee.createUser(dto);

        assertEquals(new UserDto(generatedId, "a@example.com", "A"), result);
        ArgumentCaptor<Calendar> calendar = ArgumentCaptor.forClass(Calendar.class);
        verify(calendarRepository).save(calendar.capture());
        assertEquals(generatedId, calendar.getValue().getUser().getId());
    }
}
