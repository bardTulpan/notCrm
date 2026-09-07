package com.pipeline.crm.auth;

import com.pipeline.crm.user.Role;

import java.util.UUID;

public record AuthUserDto(
        UUID id,
        String username,
        String fullName,
        Role role,
        String avatarColor
) {
}
