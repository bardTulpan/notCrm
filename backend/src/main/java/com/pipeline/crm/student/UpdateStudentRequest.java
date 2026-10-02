package com.pipeline.crm.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateStudentRequest(
        @Size(max = 200) String fullName,
        @Size(max = 100) String telegramUsername,
        UUID curatorId,
        UUID cohortId,
        Instant stageEnteredAt,
        Instant startedAt,
        @Min(0) @Max(100) Integer postpayPercent,
        List<NoteDto> notes
) {
    public record NoteDto(@Size(min = 1) String text, Integer position) {
    }
}
