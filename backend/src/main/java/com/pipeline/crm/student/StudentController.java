package com.pipeline.crm.student;

import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<StudentDto> list(
            @RequestParam(required = false) UUID stageId,
            @RequestParam(required = false) UUID curatorId,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyOverdue,
            @RequestParam(required = false) String search) {
        return studentService.list(stageId, curatorId, cohortId, onlyOverdue, search, actor());
    }

    @GetMapping("/{id}")
    public StudentDto get(@PathVariable UUID id) {
        return studentService.get(id, actor());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDto create(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.create(request, actor());
    }

    @PatchMapping("/{id}")
    public StudentDto update(@PathVariable UUID id, @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(id, request, actor());
    }

    @PostMapping("/{id}/move-stage")
    public StudentDto moveStage(@PathVariable UUID id, @Valid @RequestBody MoveStageRequest request) {
        return studentService.moveStage(id, request, actor());
    }

    @PostMapping("/{id}/pause")
    public StudentDto pause(@PathVariable UUID id) {
        return studentService.pause(id, actor());
    }

    @PostMapping("/{id}/resume")
    public StudentDto resume(@PathVariable UUID id) {
        return studentService.resume(id, actor());
    }

    @PostMapping("/{id}/assign-curator")
    public StudentDto assignCurator(@PathVariable UUID id, @Valid @RequestBody AssignCuratorRequest request) {
        return studentService.assignCurator(id, request, actor());
    }

    @GetMapping("/{id}/history")
    public StudentHistoryDto history(@PathVariable UUID id) {
        return studentService.history(id, actor());
    }

    @GetMapping("/{id}/comments")
    public List<CommentDto> comments(@PathVariable UUID id) {
        return studentService.comments(id, actor());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable UUID id, @Valid @RequestBody CreateCommentRequest request) {
        return studentService.addComment(id, request, actor());
    }

    @PatchMapping("/{id}/comments/{commentId}")
    public CommentDto updateComment(@PathVariable UUID id, @PathVariable UUID commentId, @Valid @RequestBody UpdateCommentRequest request) {
        return studentService.updateComment(id, commentId, request, actor());
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable UUID id, @PathVariable UUID commentId) {
        studentService.deleteComment(id, commentId, actor());
    }

    private CurrentUser actor() {
        return securityUtils.currentUser();
    }
}
