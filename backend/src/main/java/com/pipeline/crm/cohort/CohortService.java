package com.pipeline.crm.cohort;

import com.pipeline.crm.audit.AuditService;
import com.pipeline.crm.common.exception.ConflictException;
import com.pipeline.crm.common.exception.NotFoundException;
import com.pipeline.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;
    private final AuditService auditService;

    public List<CohortDto> list() {
        return cohortRepository.findByArchivedAtIsNullOrderByStartDateDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public CohortDto create(CreateCohortRequest request, CurrentUser actor) {
        if (cohortRepository.existsByName(request.name())) {
            throw new ConflictException("Cohort name already exists");
        }
        if (cohortRepository.existsByStartDate(request.startDate())) {
            throw new ConflictException("Cohort start date already exists");
        }
        Cohort cohort = new Cohort();
        cohort.setName(request.name());
        cohort.setStartDate(request.startDate());
        cohortRepository.save(cohort);
        auditService.log(actor.id(), "cohort", cohort.getId(), "create", null, toMap(cohort));
        return toDto(cohort);
    }

    @Transactional
    public CohortDto update(UUID id, UpdateCohortRequest request, CurrentUser actor) {
        Cohort cohort = require(id);
        if (request.name() != null) {
            cohort.setName(request.name());
        }
        if (request.startDate() != null) {
            cohort.setStartDate(request.startDate());
        }
        cohortRepository.save(cohort);
        auditService.log(actor.id(), "cohort", id, "update", null, toMap(cohort));
        return toDto(cohort);
    }

    @Transactional
    public void archive(UUID id, CurrentUser actor) {
        Cohort cohort = require(id);
        cohort.setArchivedAt(Instant.now());
        cohortRepository.save(cohort);
        auditService.log(actor.id(), "cohort", id, "archive", null, toMap(cohort));
    }

    private static final String[] MONTHS_RU = {
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    /**
     * Returns the cohort for the calendar month of {@code date} (UTC), creating it ("Январь 2026",
     * starting on the 1st) when none exists yet — so a student starting in a month nobody set up
     * a cohort for still lands in the cohort funnel instead of being left unassigned.
     */
    @Transactional
    public UUID resolveForMonth(Instant date, CurrentUser actor) {
        LocalDate day = date.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate first = day.withDayOfMonth(1);
        LocalDate last = day.withDayOfMonth(day.lengthOfMonth());

        var existing = cohortRepository.findFirstByArchivedAtIsNullAndStartDateBetweenOrderByStartDateAsc(first, last);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        // An archived cohort on the 1st would collide with the unique start_date: bring it back instead.
        var archived = cohortRepository.findByStartDate(first);
        if (archived.isPresent()) {
            Cohort cohort = archived.get();
            cohort.setArchivedAt(null);
            cohortRepository.save(cohort);
            auditService.log(actor.id(), "cohort", cohort.getId(), "restore", null, toMap(cohort));
            return cohort.getId();
        }

        Cohort cohort = new Cohort();
        cohort.setName(MONTHS_RU[first.getMonthValue() - 1] + " " + first.getYear());
        cohort.setStartDate(first);
        cohortRepository.save(cohort);
        auditService.log(actor.id(), "cohort", cohort.getId(), "create", null, toMap(cohort));
        return cohort.getId();
    }

    public Cohort require(UUID id) {
        return cohortRepository.findById(id)
                .filter(c -> c.getArchivedAt() == null)
                .orElseThrow(() -> new NotFoundException("Cohort not found"));
    }

    private CohortDto toDto(Cohort cohort) {
        return new CohortDto(cohort.getId(), cohort.getName(), cohort.getStartDate(), cohort.getArchivedAt());
    }

    private java.util.Map<String, Object> toMap(Cohort cohort) {
        return java.util.Map.of(
                "name", cohort.getName(),
                "startDate", cohort.getStartDate().toString());
    }
}
