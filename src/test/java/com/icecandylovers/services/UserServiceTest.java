package com.icecandylovers.services;

import com.icecandylovers.entities.User;
import com.icecandylovers.exceptions.DuplicateResourceException;
import com.icecandylovers.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "encodedPassword123", "ROLE_USER");
        testUser.setId(1L);
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should return user when username exists")
        void shouldReturnUserWhenUsernameExists() {
            // given
            String username = "testuser";
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

            // when
            UserDetails result = userService.loadUserByUsername(username);

            // then
            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            assertEquals("encodedPassword123", result.getPassword());
            assertFalse(result.getAuthorities().isEmpty());
            verify(userRepository, times(1)).findByUsername(username);
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when username not found")
        void shouldThrowUsernameNotFoundExceptionWhenUsernameNotFound() {
            // given
            String username = "nonexistent";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // when / then
            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> userService.loadUserByUsername(username)
            );

            assertEquals("Usu\u00e1rio n\u00e3o encontrado: nonexistent", exception.getMessage());
            verify(userRepository, times(1)).findByUsername(username);
        }
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("should register user with encoded password and ROLE_USER")
        void shouldRegisterUserWithEncodedPasswordAndRoleUser() {
            // given
            String username = "newuser";
            String rawPassword = "password123";
            String encodedPassword = "encoded_password123";

            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            userService.registerUser(username, rawPassword);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertEquals("newuser", savedUser.getUsername());
            assertEquals(encodedPassword, savedUser.getPassword());
            assertEquals("ROLE_USER", savedUser.getRole());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when username already exists")
        void shouldThrowDuplicateResourceExceptionWhenUsernameAlreadyExists() {
            // given
            String username = "existinguser";
            String password = "password123";

            when(userRepository.existsByUsername(username)).thenReturn(true);

            // when / then
            DuplicateResourceException exception = assertThrows(
                    DuplicateResourceException.class,
                    () -> userService.registerUser(username, password)
            );

            assertEquals("Nome de usuario ja esta em uso", exception.getMessage());
            verify(userRepository, times(1)).existsByUsername(username);
            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("should verify password is encoded before saving")
        void shouldVerifyPasswordIsEncodedBeforeSaving() {
            // given
            String username = "anotheruser";
            String rawPassword = "mySecret456";
            String encodedPassword = "$2a$10$encodedHashValue";

            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            userService.registerUser(username, rawPassword);

            // then
            verify(passwordEncoder, times(1)).encode(rawPassword);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertNotEquals(rawPassword, savedUser.getPassword());
            assertEquals(encodedPassword, savedUser.getPassword());
        }
    }
}
