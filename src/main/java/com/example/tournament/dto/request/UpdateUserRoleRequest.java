package com.example.tournament.dto.request;

import com.example.tournament.entity.Role;
import jakarta.validation.constraints.NotNull;

public class UpdateUserRoleRequest {

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;

    public UpdateUserRoleRequest() {
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
