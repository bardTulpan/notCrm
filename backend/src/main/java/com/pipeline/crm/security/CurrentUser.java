package com.pipeline.crm.security;

import com.pipeline.crm.user.Role;

import java.util.UUID;

public record CurrentUser(UUID id, String username, Role role, boolean seeLeads, boolean seeStats, boolean canReassign) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
