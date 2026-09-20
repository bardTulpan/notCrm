package com.pipeline.crm.statistics;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CohortStats(
        UUID cohortId,
        String name,
        LocalDate startDate,
        long total,
        List<Integer> reachedCounts,
        Integer plannedStagePosition,
        long onTrackCount,
        long behindCount
) {
}
