package com.pipeline.crm.statistics;

import com.pipeline.crm.common.exception.ForbiddenException;
import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final SecurityUtils securityUtils;

    // curatorIds narrows an admin's view to the chosen curators; it is ignored for curators,
    // who only ever see their own students.
    @GetMapping("/overview")
    public StatsOverview overview(@RequestParam(required = false) List<UUID> curatorIds) {
        return statisticsService.overview(actor(), curatorIds);
    }

    @GetMapping("/stages")
    public List<StageStats> stages(@RequestParam(required = false) List<UUID> curatorIds) {
        return statisticsService.stages(actor(), curatorIds);
    }

    @GetMapping("/curators")
    public List<CuratorStats> curators(@RequestParam(required = false) List<UUID> curatorIds) {
        return statisticsService.curators(actor(), curatorIds);
    }

    @GetMapping("/curator-workload")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public List<CuratorWorkload> curatorWorkload() {
        return statisticsService.workload();
    }

    @GetMapping("/overdue-students")
    public List<OverdueStudent> overdueStudents(@RequestParam(required = false) List<UUID> curatorIds) {
        return statisticsService.overdueStudents(actor(), curatorIds);
    }

    @GetMapping("/cohorts")
    public List<CohortStats> cohorts() {
        return statisticsService.cohorts(actor());
    }

    @GetMapping("/cohorts/{id}")
    public CohortStats cohortDetail(@PathVariable UUID id) {
        return statisticsService.cohortDetail(id, actor());
    }

    private CurrentUser actor() {
        CurrentUser user = securityUtils.currentUser();
        if (!user.isAdmin() && !user.seeStats()) {
            throw new ForbiddenException("No access to statistics");
        }
        return user;
    }
}
