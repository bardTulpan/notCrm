package com.pipeline.crm.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {

    List<Student> findByCuratorId(UUID curatorId);

    List<Student> findByCurrentStageIdAndDeletedAtIsNullOrderByStagePositionAsc(UUID currentStageId);

    @Query("SELECT COALESCE(MAX(s.stagePosition), -1) + 1 FROM Student s WHERE s.currentStageId = :stageId AND s.deletedAt IS NULL")
    int nextStagePosition(@Param("stageId") UUID stageId);
}
