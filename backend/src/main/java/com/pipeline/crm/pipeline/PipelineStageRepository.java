package com.pipeline.crm.pipeline;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {

    @Query("SELECT s FROM PipelineStage s WHERE s.isActive = true ORDER BY s.position ASC")
    List<PipelineStage> findAllActiveOrdered();

    Optional<PipelineStage> findFirstByIsActiveTrueOrderByPositionAsc();

    boolean existsByPosition(Integer position);
}
