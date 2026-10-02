package com.pipeline.crm.lead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateLeadRequest(
        @Size(max = 200) String name,
        @Size(max = 100) String telegramUsername,
        @Size(max = 255) String priceDescription,
        @Min(0) @Max(100) Integer postpayPercent,
        Instant nextPingAt,
        UUID curatorId,
        List<NoteDto> notes
) {
    public record NoteDto(@Size(min = 1) String text, Integer position) {
    }
}
