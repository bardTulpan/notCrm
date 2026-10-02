package com.pipeline.crm.statistics;

import java.util.UUID;

/** One row of the founder's "Нагрузка кураторов" table. */
public record CuratorWorkload(
        UUID curatorId,
        String name,
        String avatarColor,
        boolean blocked,
        long students,
        long green,
        long yellow,
        long red,
        long paused,
        long activeLeads,
        long overduePings,
        long newLast30Days,
        int sharePct
) {
}
