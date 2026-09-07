package com.pipeline.crm.pipeline;

import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipeline-stages")
@RequiredArgsConstructor
public class PipelineStageController {

    private final PipelineStageService stageService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<StageDto> list() {
        return stageService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StageDto create(@Valid @RequestBody CreateStageRequest request) {
        return stageService.create(request, actor());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StageDto update(@PathVariable UUID id, @Valid @RequestBody UpdateStageRequest request) {
        return stageService.update(id, request, actor());
    }

    @PutMapping("/order")
    @PreAuthorize("hasRole('ADMIN')")
    public void reorder(@RequestBody ReorderStagesRequest request) {
        stageService.reorder(request, actor());
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        stageService.archive(id, actor());
    }

    private CurrentUser actor() {
        return securityUtils.currentUser();
    }
}
