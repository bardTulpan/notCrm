package com.pipeline.crm.user;

import com.pipeline.crm.security.SecurityUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

public final class UserDtos {

    private UserDtos() {
    }

    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getAvatarColor(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    public static final Function<User, UserDto> MAPPER = UserDtos::toDto;
}
