package com.pipeline.crm.lead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    List<Lead> findByAssignedCuratorId(UUID curatorId);

    boolean existsByConvertedStudentId(UUID studentId);
}
