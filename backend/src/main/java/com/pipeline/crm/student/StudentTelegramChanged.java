package com.pipeline.crm.student;

import java.util.UUID;

/** Published when a student gets or changes a Telegram handle; their photo is (re)fetched after the commit. */
public record StudentTelegramChanged(UUID studentId) {
}
