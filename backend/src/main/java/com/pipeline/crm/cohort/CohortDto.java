package com.pipeline.crm.cohort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CohortDto(
        UUID id,
        String name,
        LocalDate startDate,
        Instant archivedAt
) {
}
