package com.pipeline.crm.student;

import java.util.List;

public final class StudentDtos {

    private StudentDtos() {
    }

    /** For a just-created student: their photo is fetched after the commit, so there's no version yet. */
    public static StudentDto toDto(Student student, Integer currentStageNormDays) {
        List<StudentDto.NoteDto> notes = student.getNotes().stream()
                .map(n -> new StudentDto.NoteDto(n.getId(), n.getText(), n.getPosition()))
                .toList();
        return new StudentDto(
                student.getId(),
                student.getFullName(),
                student.getTelegramUsername(),
                student.getSourceLeadId(),
                student.getCurrentStageId(),
                student.getCuratorId(),
                student.getCohortId(),
                student.getStageEnteredAt(),
                student.getStartedAt(),
                student.isPaused(),
                student.getPausedAt(),
                student.getPostpayPercent(),
                student.getCreatedById(),
                HealthCalculator.health(student, currentStageNormDays),
                HealthCalculator.daysOnStage(student),
                student.getStagePosition(),
                notes,
                null);
    }
}
