package com.pipeline.crm.statistics;

import java.util.List;

public record StatsOverview(
        long total,
        long green,
        long yellow,
        long red,
        long paused
) {
}
