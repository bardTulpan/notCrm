package com.pipeline.crm.lead;

import java.time.Instant;
import java.util.UUID;

public record ConvertLeadRequest(
        UUID curatorId,
        UUID cohortId,
        Integer postpayPercent,
        Instant startedAt
) {
}
