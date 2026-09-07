package com.pipeline.crm.statistics;

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

    @GetMapping("/overview")
    public StatsOverview overview() {
        return statisticsService.overview(actor());
    }

    @GetMapping("/stages")
    public List<StageStats> stages() {
        return statisticsService.stages(actor());
    }

    @GetMapping("/curators")
    public List<CuratorStats> curators() {
        return statisticsService.curators(actor());
    }

    @GetMapping("/overdue-students")
    public List<OverdueStudent> overdueStudents() {
        return statisticsService.overdueStudents(actor());
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
        return securityUtils.currentUser();
    }
}
