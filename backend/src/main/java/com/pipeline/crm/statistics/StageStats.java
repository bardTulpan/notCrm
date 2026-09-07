package com.pipeline.crm.statistics;

import java.util.UUID;

public record StageStats(
        UUID stageId,
        String name,
        Integer normDays,
        long onStage,
        long stuck
) {
}
