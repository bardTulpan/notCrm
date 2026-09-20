package com.pipeline.crm.student;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentDto(
        UUID id,
        String fullName,
        UUID sourceLeadId,
        UUID currentStageId,
        UUID curatorId,
        UUID cohortId,
        Instant stageEnteredAt,
        Instant startedAt,
        boolean isPaused,
        Instant pausedAt,
        Integer postpayPercent,
        UUID createdById,
        String health,
        long daysOnStage,
        List<NoteDto> notes
) {
    public record NoteDto(UUID id, String text, Integer position) {
    }
}
