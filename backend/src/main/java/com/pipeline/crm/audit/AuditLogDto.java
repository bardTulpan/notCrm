package com.pipeline.crm.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        Instant createdAt,
        UUID actorId,
        String actorName,
        String entityType,
        UUID entityId,
        String entityName,
        String action,
        String description
) {
}
