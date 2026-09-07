package com.pipeline.crm.lead;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LeadDto(
        UUID id,
        String name,
        String telegramUsername,
        String priceDescription,
        Integer postpayPercent,
        Instant nextPingAt,
        LeadStatus status,
        UUID assignedCuratorId,
        UUID createdById,
        UUID convertedStudentId,
        Instant archivedAt,
        List<NoteDto> notes
) {
    public record NoteDto(UUID id, String text, Integer position) {
    }
}
