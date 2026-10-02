package com.pipeline.crm.lead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.UUID;

public record ConvertLeadRequest(
        UUID curatorId,
        UUID cohortId,
        @Min(0) @Max(100) Integer postpayPercent,
        Instant startedAt
) {
}
