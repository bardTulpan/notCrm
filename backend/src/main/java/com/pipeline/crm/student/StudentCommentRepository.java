package com.pipeline.crm.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentCommentRepository extends JpaRepository<StudentComment, UUID> {

    List<StudentComment> findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID studentId);
}
