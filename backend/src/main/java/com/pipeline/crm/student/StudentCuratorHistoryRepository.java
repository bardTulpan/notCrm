package com.pipeline.crm.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentCuratorHistoryRepository extends JpaRepository<StudentCuratorHistory, UUID> {

    List<StudentCuratorHistory> findByStudentIdOrderByChangedAtAsc(UUID studentId);
}
