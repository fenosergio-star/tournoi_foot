package com.example.tournament.service;

import com.example.tournament.dto.request.CreateUserRequest;
import com.example.tournament.dto.response.UserResponse;
import com.example.tournament.entity.Role;

import java.util.List;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateRole(Long id, Role role, String currentUsername);

    void resetPassword(Long id, String newPassword);

    void deleteUser(Long id, String currentUsername);
}
