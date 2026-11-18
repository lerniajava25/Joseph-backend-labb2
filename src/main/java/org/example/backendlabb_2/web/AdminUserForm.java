package org.example.backendlabb_2.web;

import jakarta.validation.constraints.NotNull;
import org.example.backendlabb_2.user.Role;

public class AdminUserForm extends RegistrationForm {
    @NotNull(message = "Välj en roll.")
    private Role role = Role.USER;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
