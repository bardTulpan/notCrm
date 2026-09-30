package com.pipeline.crm.student;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** beforeStudentId: id of the card to place directly before within the target stage, or null to append at the end. */
public record ReorderRequest(@NotNull UUID stageId, UUID beforeStudentId) {
}
