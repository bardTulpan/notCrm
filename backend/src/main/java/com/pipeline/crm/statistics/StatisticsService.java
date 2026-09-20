package com.pipeline.crm.statistics;

import com.pipeline.crm.cohort.Cohort;
import com.pipeline.crm.cohort.CohortRepository;
import com.pipeline.crm.pipeline.PipelineStage;
import com.pipeline.crm.pipeline.PipelineStageRepository;
import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.student.HealthCalculator;
import com.pipeline.crm.student.Student;
import com.pipeline.crm.student.StudentRepository;
import com.pipeline.crm.user.User;
import com.pipeline.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StudentRepository studentRepository;
    private final PipelineStageRepository stageRepository;
    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;

    public StatsOverview overview(CurrentUser user) {
        List<Student> students = visibleStudents(user);
        long green = 0, yellow = 0, red = 0, paused = 0;
        for (Student s : students) {
            String h = health(s);
            switch (h) {
                case "green" -> green++;
                case "yellow" -> yellow++;
                case "red" -> red++;
                default -> paused++;
            }
        }
        return new StatsOverview(students.size(), green, yellow, red, paused);
    }

    public List<StageStats> stages(CurrentUser user) {
        List<Student> students = visibleStudents(user);
        List<PipelineStage> stages = stageRepository.findAllActiveOrdered();
        List<StageStats> result = new ArrayList<>();
        for (PipelineStage stage : stages) {
            List<Student> onStage = students.stream()
                    .filter(s -> s.getCurrentStageId().equals(stage.getId()))
                    .toList();
            long stuck = onStage.stream().filter(s -> "red".equals(health(s))).count();
            result.add(new StageStats(stage.getId(), stage.getName(), stage.getNormDays(), onStage.size(), stuck));
        }
        return result;
    }

    public List<CuratorStats> curators(CurrentUser user) {
        if (!user.isAdmin()) {
            return List.of(ownCuratorStats(user));
        }
        List<CuratorStats> result = new ArrayList<>();
        for (User curator : userRepository.findAllActive().stream()
                .filter(u -> u.getRole() == com.pipeline.crm.user.Role.CURATOR).toList()) {
            result.add(curatorStats(curator));
        }
        return result;
    }

    public List<OverdueStudent> overdueStudents(CurrentUser user) {
        List<Student> students = visibleStudents(user).stream()
                .filter(this::isOverdue)
                .toList();
        List<OverdueStudent> result = new ArrayList<>();
        for (Student s : students) {
            PipelineStage stage = stageRepository.findById(s.getCurrentStageId()).orElse(null);
            User curator = userRepository.findById(s.getCuratorId()).orElse(null);
            long days = HealthCalculator.daysOnStage(s);
            result.add(new OverdueStudent(
                    s.getId(), s.getFullName(),
                    s.getCurrentStageId(),
                    stage != null ? stage.getName() : null,
                    stage != null ? stage.getNormDays() : null,
                    s.getCuratorId(),
                    curator != null ? curator.getFullName() : null,
                    days));
        }
        result.sort((a, b) -> Long.compare(b.daysOnStage(), a.daysOnStage()));
        return result;
    }

    public List<CohortStats> cohorts(CurrentUser user) {
        List<Student> students = visibleStudents(user);
        List<PipelineStage> stages = stageRepository.findAllActiveOrdered();
        List<CohortStats> result = new ArrayList<>();
        for (Cohort cohort : cohortRepository.findByArchivedAtIsNullOrderByStartDateDesc()) {
            List<Student> inCohort = students.stream()
                    .filter(s -> cohort.getId().equals(s.getCohortId()))
                    .toList();
            if (inCohort.isEmpty()) {
                continue;
            }
            result.add(buildCohortStats(cohort.getId(), cohort.getName(), cohort.getStartDate(), inCohort, stages));
        }
        return result;
    }

    public CohortStats cohortDetail(UUID cohortId, CurrentUser user) {
        List<Student> students = visibleStudents(user).stream()
                .filter(s -> cohortId.equals(s.getCohortId()))
                .toList();
        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        List<PipelineStage> stages = stageRepository.findAllActiveOrdered();
        return buildCohortStats(cohortId, cohort != null ? cohort.getName() : null,
                cohort != null ? cohort.getStartDate() : null, students, stages);
    }

    private CohortStats buildCohortStats(UUID cohortId, String name, java.time.LocalDate startDate,
                                          List<Student> inCohort, List<PipelineStage> stages) {
        List<Integer> reached = new ArrayList<>();
        for (PipelineStage stage : stages) {
            int count = (int) inCohort.stream()
                    .filter(s -> stageIndex(s) >= stage.getPosition())
                    .count();
            reached.add(count);
        }

        Integer plannedStagePosition = null;
        if (startDate != null) {
            long elapsed = ChronoUnit.DAYS.between(startDate.atStartOfDay(ZoneOffset.UTC).toInstant(), Instant.now());
            plannedStagePosition = plannedStagePositionForElapsedDays(elapsed, stages);
        }

        long onTrack = 0;
        long behind = 0;
        for (Student s : inCohort) {
            long individualElapsed = ChronoUnit.DAYS.between(s.getStartedAt(), Instant.now());
            int individualPlanned = plannedStagePositionForElapsedDays(individualElapsed, stages);
            if (stageIndex(s) >= individualPlanned) {
                onTrack++;
            } else {
                behind++;
            }
        }

        return new CohortStats(cohortId, name, startDate, inCohort.size(), reached,
                plannedStagePosition, onTrack, behind);
    }

    private int plannedStagePositionForElapsedDays(long elapsedDays, List<PipelineStage> stagesOrdered) {
        long sum = 0;
        for (PipelineStage stage : stagesOrdered) {
            if (stage.getNormDays() == null) {
                return stage.getPosition();
            }
            sum += stage.getNormDays();
            if (elapsedDays < sum) {
                return stage.getPosition();
            }
        }
        return stagesOrdered.isEmpty() ? 0 : stagesOrdered.get(stagesOrdered.size() - 1).getPosition();
    }

    private List<Student> visibleStudents(CurrentUser user) {
        if (user.isAdmin()) {
            return studentRepository.findAll().stream().filter(s -> s.getDeletedAt() == null).toList();
        }
        return studentRepository.findByCuratorId(user.id()).stream().filter(s -> s.getDeletedAt() == null).toList();
    }

    private CuratorStats ownCuratorStats(CurrentUser user) {
        User curator = userRepository.findById(user.id()).orElse(null);
        List<Student> mine = visibleStudents(user);
        return buildStats(curator, mine);
    }

    private CuratorStats curatorStats(User curator) {
        List<Student> mine = studentRepository.findByCuratorId(curator.getId()).stream()
                .filter(s -> s.getDeletedAt() == null)
                .toList();
        return buildStats(curator, mine);
    }

    private CuratorStats buildStats(User curator, List<Student> mine) {
        long stuck = mine.stream().filter(s -> "red".equals(health(s))).count();
        long withNorm = mine.stream().filter(s -> !s.isPaused() && normDays(s) != null).count();
        double avg = 0;
        if (withNorm > 0) {
            double sum = 0;
            for (Student s : mine) {
                Integer norm = normDays(s);
                if (s.isPaused() || norm == null) continue;
                long days = ChronoUnit.DAYS.between(s.getStageEnteredAt(), Instant.now());
                sum += (double) days / norm;
            }
            avg = sum / withNorm;
        }
        int avgPct = (int) Math.round(avg * 100);
        return new CuratorStats(
                curator.getId(), curator.getFullName(), curator.getAvatarColor(),
                mine.size(), stuck, avgPct);
    }

    private String health(Student s) {
        return HealthCalculator.health(s, normDays(s));
    }

    private boolean isOverdue(Student s) {
        return HealthCalculator.isOverdue(s, normDays(s));
    }

    private Integer normDays(Student s) {
        PipelineStage stage = stageRepository.findById(s.getCurrentStageId()).orElse(null);
        return stage != null ? stage.getNormDays() : null;
    }

    private int stageIndex(Student s) {
        PipelineStage stage = stageRepository.findById(s.getCurrentStageId()).orElse(null);
        return stage != null ? stage.getPosition() : -1;
    }
}
