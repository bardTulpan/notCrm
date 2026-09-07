package com.pipeline.crm.student;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        UUID id,
        UUID studentId,
        UUID authorId,
        String text,
        Instant createdAt,
        Instant updatedAt
) {
}
