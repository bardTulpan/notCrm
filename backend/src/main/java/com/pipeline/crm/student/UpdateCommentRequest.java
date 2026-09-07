package com.pipeline.crm.student;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(@NotBlank String text) {
}
