package com.pipeline.crm.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 200) String fullName,
        String avatarColor,
        Boolean seeLeads,
        Boolean seeStats,
        Boolean canReassign
) {
}
