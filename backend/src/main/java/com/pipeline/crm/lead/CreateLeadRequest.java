package com.pipeline.crm.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateLeadRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String telegramUsername,
        @Size(max = 255) String priceDescription,
        Integer postpayPercent,
        Instant nextPingAt,
        UUID curatorId,
        List<NoteDto> notes
) {
    public record NoteDto(@NotBlank String text, Integer position) {
    }
}
