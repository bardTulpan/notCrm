package com.pipeline.crm.statistics;

import java.util.UUID;

/** Paused students are counted apart from {@code active}: a pause usually means the student dropped out. */
public record StageStats(
        UUID stageId,
        String name,
        Integer normDays,
        long active,
        long paused,
        long stuck
) {
}
