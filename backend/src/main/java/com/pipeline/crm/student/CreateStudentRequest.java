package com.pipeline.crm.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateStudentRequest(
        @NotBlank @Size(max = 200) String fullName,
        UUID curatorId,
        UUID cohortId,
        @NotNull UUID currentStageId,
        Instant stageEnteredAt,
        Instant startedAt,
        Integer postpayPercent
) {
}
