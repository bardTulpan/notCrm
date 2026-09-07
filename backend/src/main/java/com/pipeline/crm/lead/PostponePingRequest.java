package com.pipeline.crm.lead;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PostponePingRequest(@NotNull Instant nextPingAt) {
}
