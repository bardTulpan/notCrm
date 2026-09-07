package com.pipeline.crm.statistics;

import java.time.Instant;
import java.util.UUID;

public record OverdueStudent(
        UUID studentId,
        String name,
        UUID stageId,
        String stageName,
        Integer normDays,
        UUID curatorId,
        String curatorName,
        long daysOnStage
) {
}
