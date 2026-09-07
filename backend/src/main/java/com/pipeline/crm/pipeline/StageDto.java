package com.pipeline.crm.pipeline;

import java.util.UUID;

public record StageDto(
        UUID id,
        String name,
        Integer position,
        Integer normDays,
        boolean isFinal,
        boolean isActive
) {
}
