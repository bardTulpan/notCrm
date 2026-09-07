package com.pipeline.crm.pipeline;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateStageRequest(
        @Size(max = 200) String name,
        @Positive Integer normDays,
        Integer position,
        Boolean isActive
) {
}
