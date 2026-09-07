package com.pipeline.crm.cohort;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCohortRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull LocalDate startDate
) {
}
