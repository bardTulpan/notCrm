package com.pipeline.crm.student;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class HealthCalculator {

    private HealthCalculator() {
    }

    public static String health(Student student, Integer normDays) {
        if (student.isPaused()) {
            return "paused";
        }
        if (normDays == null) {
            return "green";
        }
        long days = ChronoUnit.DAYS.between(student.getStageEnteredAt(), Instant.now());
        double ratio = (double) days / normDays;
        if (ratio > 1.0) {
            return "red";
        }
        if (ratio >= 0.7) {
            return "yellow";
        }
        return "green";
    }

    public static boolean isOverdue(Student student, Integer normDays) {
        if (student.isPaused()) {
            return false;
        }
        if (normDays == null) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(student.getStageEnteredAt(), Instant.now());
        return days > normDays;
    }
}
