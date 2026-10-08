package com.pipeline.crm.student;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentDto(
        UUID id,
        String fullName,
        String telegramUsername,
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
        int stagePosition,
        List<NoteDto> notes,
        /* Changes whenever the Telegram photo changes; null = no photo (show initials). */
        String avatarVersion
) {
    public record NoteDto(UUID id, String text, Integer position) {
    }
}
