package com.pipeline.crm.cohort;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CohortRepository extends JpaRepository<Cohort, UUID> {

    List<Cohort> findByArchivedAtIsNullOrderByStartDateDesc();

    boolean existsByName(String name);

    boolean existsByStartDate(java.time.LocalDate startDate);

    java.util.Optional<Cohort> findFirstByArchivedAtIsNullAndStartDateBetweenOrderByStartDateAsc(
            java.time.LocalDate from, java.time.LocalDate to);

    java.util.Optional<Cohort> findByStartDate(java.time.LocalDate startDate);

    java.util.Optional<Cohort> findByName(String name);
}
