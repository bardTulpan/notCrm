package com.pipeline.crm.pipeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateStageRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull Integer position,
        @Positive Integer normDays,
        boolean isFinal
) {
}
