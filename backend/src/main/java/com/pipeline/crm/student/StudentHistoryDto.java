package com.pipeline.crm.student;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentHistoryDto(
        List<StageEvent> stages,
        List<CuratorEvent> curators
) {
    public record StageEvent(UUID stageId, Instant enteredAt, Instant exitedAt, String changedById) {
    }

    public record CuratorEvent(UUID fromCuratorId, UUID toCuratorId, Instant changedAt) {
    }
}
