package com.pipeline.crm.student;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignCuratorRequest(@NotNull UUID curatorId) {
}
