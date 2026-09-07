package com.pipeline.crm.cohort;

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
@RequestMapping("/api/v1/cohorts")
@RequiredArgsConstructor
public class CohortController {

    private final CohortService cohortService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<CohortDto> list() {
        return cohortService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CohortDto create(@Valid @RequestBody CreateCohortRequest request) {
        return cohortService.create(request, actor());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CohortDto update(@PathVariable UUID id, @Valid @RequestBody UpdateCohortRequest request) {
        return cohortService.update(id, request, actor());
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        cohortService.archive(id, actor());
    }

    private CurrentUser actor() {
        return securityUtils.currentUser();
    }
}
