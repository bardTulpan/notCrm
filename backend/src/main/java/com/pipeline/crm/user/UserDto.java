package com.pipeline.crm.user;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String fullName,
        String avatarColor,
        Role role,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        boolean seeLeads,
        boolean seeStats,
        boolean canReassign,
        boolean canDeleteStudents
) {
}
