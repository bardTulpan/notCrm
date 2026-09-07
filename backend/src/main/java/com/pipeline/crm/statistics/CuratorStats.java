package com.pipeline.crm.statistics;

import java.util.UUID;

public record CuratorStats(
        UUID curatorId,
        String name,
        String avatarColor,
        long count,
        long stuck,
        int avgPct
) {
}
