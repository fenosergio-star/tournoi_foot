package com.example.tournament.service;

import com.example.tournament.dto.request.CreateUserRequest;
import com.example.tournament.entity.Role;
import com.example.tournament.entity.UserAccount;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.mapper.UserMapper;
import com.example.tournament.repository.UserAccountRepository;
import com.example.tournament.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserAccount user(Long id, String username, Role role) {
        UserAccount u = new UserAccount();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        return u;
    }

    @Test
    void createUser_rejectsDuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("dup");
        request.setPassword("secret123");
        request.setRole(Role.USER);
        when(userRepository.existsByUsernameIgnoreCase("dup")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_rejectsSelfDeletion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));

        assertThatThrownBy(() -> userService.deleteUser(1L, "admin"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("votre propre compte");
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_rejectsLastAdministrator() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(1L, "someone"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("dernier administrateur");
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_allowsRemovingNonLastAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);

        userService.deleteUser(1L, "someone");

        verify(userRepository).delete(any(UserAccount.class));
    }

    @Test
    void updateRole_rejectsDemotingLastAdministrator() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateRole(1L, Role.USER, "admin"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("dernier administrateur");
    }
}
