package com.mentify.userservice.service;

import com.mentify.userservice.entity.Role;
import com.mentify.userservice.entity.User;
import com.mentify.userservice.exception.UserNotFoundException;
import com.mentify.userservice.repository.UserRepository;
import com.mentify.userservice.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("updateUserStatus - should deactivate an active user")
    void updateUserStatus_ShouldDeactivateUser_WhenUserIsActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.updateUserStatus(1L, false);

        assertThat(testUser.isActive()).isFalse();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUserStatus - should activate an inactive user")
    void updateUserStatus_ShouldActivateUser_WhenUserIsInactive() {
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.updateUserStatus(1L, true);

        assertThat(testUser.isActive()).isTrue();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUserStatus - should throw UserNotFoundException when user does not exist")
    void updateUserStatus_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserStatus(99L, false))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserStatus - should persist the updated status")
    void updateUserStatus_ShouldPersistUpdate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminService.updateUserStatus(1L, false);

        verify(userRepository, times(1)).save(argThat(user -> !user.isActive()));
    }
}
