package com.pipeline.crm.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentStageHistoryRepository extends JpaRepository<StudentStageHistory, UUID> {

    Optional<StudentStageHistory> findByStudentIdAndExitedAtIsNull(UUID studentId);

    List<StudentStageHistory> findByStudentIdOrderByEnteredAtAsc(UUID studentId);
}
