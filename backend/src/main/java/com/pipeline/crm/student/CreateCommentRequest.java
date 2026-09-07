package com.pipeline.crm.student;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(@NotBlank String text) {
}
