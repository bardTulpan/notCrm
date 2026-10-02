package com.pipeline.crm.student;

import com.pipeline.crm.audit.AuditService;
import com.pipeline.crm.cohort.CohortService;
import com.pipeline.crm.common.exception.ConflictException;
import com.pipeline.crm.common.exception.ForbiddenException;
import com.pipeline.crm.common.exception.NotFoundException;
import com.pipeline.crm.pipeline.PipelineStage;
import com.pipeline.crm.pipeline.PipelineStageRepository;
import com.pipeline.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final PipelineStageRepository stageRepository;
    private final StudentStageHistoryRepository stageHistoryRepository;
    private final StudentCuratorHistoryRepository curatorHistoryRepository;
    private final StudentCommentRepository commentRepository;
    private final AuditService auditService;
    private final CohortService cohortService;

    public List<StudentDto> list(UUID stageId, UUID curatorId, UUID cohortId, boolean onlyOverdue, String search, CurrentUser user) {
        Specification<Student> spec = (root, query, cb) -> {
            jakarta.persistence.criteria.Predicate notDeleted = cb.isNull(root.get("deletedAt"));
            jakarta.persistence.criteria.Predicate p = notDeleted;
            if (stageId != null) p = cb.and(p, cb.equal(root.get("currentStageId"), stageId));
            if (cohortId != null) p = cb.and(p, cb.equal(root.get("cohortId"), cohortId));
            if (search != null && !search.isBlank()) {
                p = cb.and(p, cb.like(cb.lower(root.get("fullName")), "%" + search.toLowerCase() + "%"));
            }
            return p;
        };

        if (!user.isAdmin()) {
            UUID myId = user.id();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("curatorId"), myId));
        } else if (curatorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("curatorId"), curatorId));
        }

        List<Student> students = studentRepository.findAll(spec, Sort.by(Sort.Order.asc("fullName")));

        return students.stream()
                .filter(s -> !onlyOverdue || isOverdue(s))
                .map(s -> enriched(s, s.getNotes().stream()
                        .map(n -> new StudentDto.NoteDto(n.getId(), n.getText(), n.getPosition())).toList()))
                .toList();
    }

    public StudentDto get(UUID id, CurrentUser user) {
        Student student = requireFor(id, user);
        return toFullDto(student);
    }

    @Transactional
    public StudentDto create(CreateStudentRequest request, CurrentUser user) {
        PipelineStage stage = stageRepository.findById(request.currentStageId())
                .filter(PipelineStage::isActive)
                .orElseThrow(() -> new ConflictException("Stage is not active"));

        if (user.isAdmin() && request.curatorId() == null) {
            throw new com.pipeline.crm.common.exception.BadRequestException("curatorId is required");
        }
        UUID curatorId = user.isAdmin() ? request.curatorId() : user.id();

        Instant now = Instant.now();
        Student student = new Student();
        student.setFullName(request.fullName());
        student.setCurrentStageId(request.currentStageId());
        student.setCuratorId(curatorId);
        Instant startedAt = request.startedAt() != null ? request.startedAt() : now;
        student.setCohortId(request.cohortId() != null ? request.cohortId() : cohortService.resolveForMonth(startedAt, user));
        student.setStageEnteredAt(request.stageEnteredAt() != null ? request.stageEnteredAt() : now);
        student.setStartedAt(startedAt);
        student.setPostpayPercent(request.postpayPercent());
        student.setCreatedById(user.id());
        student.setStagePosition(studentRepository.nextStagePosition(request.currentStageId()));
        studentRepository.save(student);

        StudentStageHistory history = new StudentStageHistory();
        history.setStudentId(student.getId());
        history.setStageId(request.currentStageId());
        history.setEnteredAt(student.getStageEnteredAt());
        history.setChangedById(user.id());
        stageHistoryRepository.save(history);

        auditService.log(user.id(), "student", student.getId(), "create", null,
                Map.of("fullName", student.getFullName(), "stageId", stage.getId().toString(),
                        "curatorId", curatorId.toString()));
        return toFullDto(student);
    }

    @Transactional
    public StudentDto update(UUID id, UpdateStudentRequest request, CurrentUser user) {
        Student student = requireFor(id, user);
        if (request.fullName() != null) student.setFullName(request.fullName());
        if (request.postpayPercent() != null) student.setPostpayPercent(request.postpayPercent());
        if (request.startedAt() != null) student.setStartedAt(request.startedAt());
        if (request.stageEnteredAt() != null) student.setStageEnteredAt(request.stageEnteredAt());
        if (request.curatorId() != null) {
            if (!user.isAdmin() && !user.canReassign()) {
                throw new ForbiddenException("Only admin can reassign curator");
            }
            reassignCurator(student, request.curatorId(), user);
        }
        if (request.cohortId() != null) {
            if (!user.isAdmin()) {
                throw new ForbiddenException("Only admin can change cohort");
            }
            student.setCohortId(request.cohortId());
        }
        if (request.notes() != null) {
            student.getNotes().clear();
            int pos = 0;
            for (UpdateStudentRequest.NoteDto note : request.notes()) {
                StudentNote sn = new StudentNote();
                sn.setStudent(student);
                sn.setText(note.text());
                sn.setPosition(note.position() != null ? note.position() : pos++);
                student.getNotes().add(sn);
            }
        }
        studentRepository.save(student);
        return toFullDto(student);
    }

    @Transactional
    public StudentDto moveStage(UUID id, MoveStageRequest request, CurrentUser user) {
        Student student = requireFor(id, user);
        stageRepository.findById(request.stageId())
                .filter(PipelineStage::isActive)
                .orElseThrow(() -> new ConflictException("Stage is not active"));

        if (request.stageId().equals(student.getCurrentStageId())) {
            return toFullDto(student);
        }
        changeStage(student, request.stageId(), user);
        studentRepository.save(student);
        return toFullDto(student);
    }

    /**
     * Moves the student to another stage and keeps the stage history consistent.
     * If the target is the stage the student was on immediately before (an accidental drag put back,
     * or an "undo"), the visit is collapsed: the intermediate history row is dropped and the earlier one
     * is reopened, so "days on stage" carries on from the original entry instead of restarting at zero.
     */
    private void changeStage(Student student, UUID newStageId, CurrentUser user) {
        UUID fromStageId = student.getCurrentStageId();
        var open = stageHistoryRepository.findByStudentIdAndExitedAtIsNull(student.getId()).orElse(null);

        StudentStageHistory previous = null;
        if (open != null) {
            previous = stageHistoryRepository.findByStudentIdOrderByEnteredAtAsc(student.getId()).stream()
                    .filter(h -> h.getExitedAt() != null)
                    .reduce((a, b) -> b)
                    .filter(h -> h.getStageId().equals(newStageId))
                    .orElse(null);
        }

        boolean restored = previous != null;
        if (restored) {
            stageHistoryRepository.delete(open);
            previous.setExitedAt(null);
            stageHistoryRepository.save(previous);
            student.setCurrentStageId(newStageId);
            student.setStageEnteredAt(previous.getEnteredAt());
        } else {
            Instant now = Instant.now();
            if (open != null) {
                open.setExitedAt(now);
                stageHistoryRepository.save(open);
            }
            student.setCurrentStageId(newStageId);
            student.setStageEnteredAt(now);

            StudentStageHistory history = new StudentStageHistory();
            history.setStudentId(student.getId());
            history.setStageId(newStageId);
            history.setEnteredAt(now);
            history.setChangedById(user.id());
            stageHistoryRepository.save(history);
        }

        Map<String, Object> after = new java.util.LinkedHashMap<>();
        after.put("fromStageId", fromStageId.toString());
        after.put("toStageId", newStageId.toString());
        if (restored) after.put("timerRestored", true);
        auditService.log(user.id(), "student", student.getId(), "move-stage", null, after);
    }

    /**
     * Drives the Kanban drag gesture: changes stage (if needed) and always places the card at the
     * requested position within the target stage. Stage transitions are audited as "move-stage";
     * a same-stage drag that actually changes the order is audited as "reorder".
     */
    @Transactional
    public StudentDto reorder(UUID id, ReorderRequest request, CurrentUser user) {
        Student student = requireFor(id, user);
        PipelineStage stage = stageRepository.findById(request.stageId())
                .filter(PipelineStage::isActive)
                .orElseThrow(() -> new ConflictException("Stage is not active"));

        boolean stageChanged = !request.stageId().equals(student.getCurrentStageId());
        if (stageChanged) {
            changeStage(student, request.stageId(), user);
        }

        List<Student> stageStudents = studentRepository
                .findByCurrentStageIdAndDeletedAtIsNullOrderByStagePositionAsc(request.stageId());
        List<UUID> orderBefore = stageStudents.stream().map(Student::getId).toList();
        List<Student> siblings = stageStudents.stream()
                .filter(s -> !s.getId().equals(student.getId()))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        int insertAt = siblings.size();
        if (request.beforeStudentId() != null) {
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i).getId().equals(request.beforeStudentId())) {
                    insertAt = i;
                    break;
                }
            }
        }
        siblings.add(insertAt, student);

        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setStagePosition(i);
        }
        studentRepository.saveAll(siblings);

        if (!stageChanged && !orderBefore.equals(siblings.stream().map(Student::getId).toList())) {
            auditService.log(user.id(), "student", id, "reorder", null, Map.of("stageId", request.stageId().toString()));
        }
        return toFullDto(student);
    }

    @Transactional
    public StudentDto pause(UUID id, CurrentUser user) {
        Student student = requireFor(id, user);
        student.setPaused(true);
        student.setPausedAt(Instant.now());
        studentRepository.save(student);
        auditService.log(user.id(), "student", id, "pause", null, null);
        return toFullDto(student);
    }

    @Transactional
    public StudentDto resume(UUID id, CurrentUser user) {
        Student student = requireFor(id, user);
        student.setPaused(false);
        student.setPausedAt(null);
        studentRepository.save(student);
        auditService.log(user.id(), "student", id, "resume", null, null);
        return toFullDto(student);
    }

    @Transactional
    public StudentDto assignCurator(UUID id, AssignCuratorRequest request, CurrentUser user) {
        Student student = requireFor(id, user);
        if (!user.isAdmin() && !user.canReassign()) {
            throw new ForbiddenException("Only admin can reassign curator");
        }
        reassignCurator(student, request.curatorId(), user);
        studentRepository.save(student);
        return toFullDto(student);
    }

    public StudentHistoryDto history(UUID id, CurrentUser user) {
        Student student = requireFor(id, user);
        List<StudentHistoryDto.StageEvent> stages = stageHistoryRepository.findByStudentIdOrderByEnteredAtAsc(id).stream()
                .map(h -> new StudentHistoryDto.StageEvent(h.getStageId(), h.getEnteredAt(), h.getExitedAt(), h.getChangedById().toString()))
                .toList();
        List<StudentHistoryDto.CuratorEvent> curators = curatorHistoryRepository.findByStudentIdOrderByChangedAtAsc(id).stream()
                .map(c -> new StudentHistoryDto.CuratorEvent(c.getFromCuratorId(), c.getToCuratorId(), c.getChangedAt()))
                .toList();
        return new StudentHistoryDto(stages, curators);
    }

    public List<CommentDto> comments(UUID id, CurrentUser user) {
        requireFor(id, user);
        return commentRepository.findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(id).stream()
                .map(this::toCommentDto)
                .toList();
    }

    @Transactional
    public CommentDto addComment(UUID id, CreateCommentRequest request, CurrentUser user) {
        Student student = requireFor(id, user);
        StudentComment comment = new StudentComment();
        comment.setStudentId(id);
        comment.setAuthorId(user.id());
        comment.setText(request.text());
        commentRepository.save(comment);
        return toCommentDto(comment);
    }

    @Transactional
    public CommentDto updateComment(UUID studentId, UUID commentId, UpdateCommentRequest request, CurrentUser user) {
        Student student = requireFor(studentId, user);
        StudentComment comment = commentRepository.findById(commentId)
                .filter(c -> c.getDeletedAt() == null && c.getStudentId().equals(studentId))
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        if (!user.isAdmin() && !user.id().equals(comment.getAuthorId())) {
            throw new ForbiddenException("You can only edit your own comments");
        }
        comment.setText(request.text());
        commentRepository.save(comment);
        return toCommentDto(comment);
    }

    @Transactional
    public void deleteComment(UUID studentId, UUID commentId, CurrentUser user) {
        requireFor(studentId, user);
        StudentComment comment = commentRepository.findById(commentId)
                .filter(c -> c.getDeletedAt() == null && c.getStudentId().equals(studentId))
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        if (!user.isAdmin() && !user.id().equals(comment.getAuthorId())) {
            throw new ForbiddenException("You can only delete your own comments");
        }
        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
    }

    private void reassignCurator(Student student, UUID newCuratorId, CurrentUser user) {
        if (newCuratorId.equals(student.getCuratorId())) {
            return;
        }
        StudentCuratorHistory history = new StudentCuratorHistory();
        history.setStudentId(student.getId());
        history.setFromCuratorId(student.getCuratorId());
        history.setToCuratorId(newCuratorId);
        history.setChangedById(user.id());
        curatorHistoryRepository.save(history);
        student.setCuratorId(newCuratorId);
        auditService.log(user.id(), "student", student.getId(), "assign-curator", null,
                Map.of("fromCuratorId", String.valueOf(history.getFromCuratorId()), "toCuratorId", newCuratorId.toString()));
    }

    private Student requireFor(UUID id, CurrentUser user) {
        Student student = studentRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        if (!user.isAdmin() && !user.id().equals(student.getCuratorId())) {
            throw new NotFoundException("Student not found");
        }
        return student;
    }

    private boolean isOverdue(Student student) {
        PipelineStage stage = stageRepository.findById(student.getCurrentStageId()).orElse(null);
        Integer norm = stage != null ? stage.getNormDays() : null;
        return HealthCalculator.isOverdue(student, norm);
    }

    private StudentDto enriched(Student student, List<StudentDto.NoteDto> notes) {
        return withHealth(student, notes);
    }

    private StudentDto withHealth(Student student, List<StudentDto.NoteDto> notes) {
        PipelineStage stage = stageRepository.findById(student.getCurrentStageId()).orElse(null);
        Integer norm = stage != null ? stage.getNormDays() : null;
        String health = HealthCalculator.health(student, norm);
        return new StudentDto(
                student.getId(), student.getFullName(), student.getSourceLeadId(),
                student.getCurrentStageId(), student.getCuratorId(), student.getCohortId(),
                student.getStageEnteredAt(), student.getStartedAt(), student.isPaused(),
                student.getPausedAt(), student.getPostpayPercent(), student.getCreatedById(),
                health, HealthCalculator.daysOnStage(student), student.getStagePosition(), notes);
    }

    private StudentDto toFullDto(Student student) {
        List<StudentDto.NoteDto> notes = student.getNotes().stream()
                .map(n -> new StudentDto.NoteDto(n.getId(), n.getText(), n.getPosition()))
                .toList();
        return withHealth(student, notes);
    }

    private CommentDto toCommentDto(StudentComment comment) {
        return new CommentDto(comment.getId(), comment.getStudentId(), comment.getAuthorId(),
                comment.getText(), comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
