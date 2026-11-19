package com.example.tournament.mapper;

import com.example.tournament.dto.response.UserResponse;
import com.example.tournament.entity.Role;
import com.example.tournament.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserAccount toEntity(String username, String encodedPassword, Role role) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    public UserResponse toResponse(UserAccount entity) {
        if (entity == null) return null;
        UserResponse response = new UserResponse();
        response.setId(entity.getId());
        response.setUsername(entity.getUsername());
        response.setRole(entity.getRole());
        response.setEnabled(entity.isEnabled());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
