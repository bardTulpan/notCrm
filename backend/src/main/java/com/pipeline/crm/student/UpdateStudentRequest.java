package com.pipeline.crm.student;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateStudentRequest(
        @Size(max = 200) String fullName,
        UUID curatorId,
        UUID cohortId,
        Instant stageEnteredAt,
        Instant startedAt,
        Integer postpayPercent,
        List<NoteDto> notes
) {
    public record NoteDto(@Size(min = 1) String text, Integer position) {
    }
}
