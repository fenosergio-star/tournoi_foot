package com.example.tournament.service.impl;

import com.example.tournament.dto.request.CreateUserRequest;
import com.example.tournament.dto.response.UserResponse;
import com.example.tournament.entity.Role;
import com.example.tournament.entity.UserAccount;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.UserMapper;
import com.example.tournament.repository.UserAccountRepository;
import com.example.tournament.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserAccountRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserAccountRepository userRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new ConflictException("Un utilisateur avec ce nom existe déjà : " + request.getUsername());
        }
        UserAccount user = userMapper.toEntity(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateRole(Long id, Role role, String currentUsername) {
        UserAccount user = findUser(id);
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new InvalidOperationException("Impossible de retirer le rôle du dernier administrateur");
        }
        user.setRole(role);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        UserAccount user = findUser(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        UserAccount user = findUser(id);
        if (user.getUsername().equals(currentUsername)) {
            throw new InvalidOperationException("Vous ne pouvez pas supprimer votre propre compte");
        }
        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new InvalidOperationException("Impossible de supprimer le dernier administrateur");
        }
        userRepository.delete(user);
    }

    private UserAccount findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID : " + id));
    }
}
