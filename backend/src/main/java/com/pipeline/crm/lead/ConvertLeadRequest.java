package com.pipeline.crm.lead;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ConvertLeadRequest(
        @NotNull UUID curatorId,
        UUID cohortId,
        Integer postpayPercent,
        Instant startedAt
) {
}
