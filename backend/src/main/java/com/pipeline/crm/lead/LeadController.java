package com.pipeline.crm.lead;

import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.security.SecurityUtils;
import com.pipeline.crm.student.StudentDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<LeadDto> list(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID assignedCuratorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant pingFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant pingTo) {
        return leadService.list(status, search, assignedCuratorId, pingFrom, pingTo, actor());
    }

    @GetMapping("/{id}")
    public LeadDto get(@PathVariable UUID id) {
        return leadService.get(id, actor());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadDto create(@Valid @RequestBody CreateLeadRequest request) {
        return leadService.create(request, actor());
    }

    @PatchMapping("/{id}")
    public LeadDto update(@PathVariable UUID id, @Valid @RequestBody UpdateLeadRequest request) {
        return leadService.update(id, request, actor());
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        leadService.archive(id, actor());
    }

    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable UUID id) {
        leadService.restore(id, actor());
    }

    @PostMapping("/{id}/postpone-ping")
    public LeadDto postponePing(@PathVariable UUID id, @Valid @RequestBody PostponePingRequest request) {
        return leadService.postponePing(id, request, actor());
    }

    @PostMapping("/{id}/convert")
    public StudentDto convert(@PathVariable UUID id, @Valid @RequestBody ConvertLeadRequest request) {
        return leadService.convert(id, request, actor());
    }

    private CurrentUser actor() {
        return securityUtils.currentUser();
    }
}
