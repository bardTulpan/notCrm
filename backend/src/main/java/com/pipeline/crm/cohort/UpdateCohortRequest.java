package com.pipeline.crm.cohort;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCohortRequest(
        @Size(max = 100) String name,
        LocalDate startDate
) {
}
