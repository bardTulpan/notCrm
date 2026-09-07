package com.pipeline.crm.pipeline;

import com.pipeline.crm.audit.AuditService;
import com.pipeline.crm.common.exception.ConflictException;
import com.pipeline.crm.common.exception.NotFoundException;
import com.pipeline.crm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PipelineStageService {

    private final PipelineStageRepository stageRepository;
    private final AuditService auditService;

    public List<StageDto> list() {
        return stageRepository.findAllActiveOrdered().stream().map(this::toDto).toList();
    }

    @Transactional
    public StageDto create(CreateStageRequest request, CurrentUser actor) {
        if (stageRepository.existsByPosition(request.position())) {
            throw new ConflictException("Stage position already exists");
        }
        PipelineStage stage = new PipelineStage();
        stage.setName(request.name());
        stage.setPosition(request.position());
        stage.setNormDays(request.normDays());
        stage.setFinal(request.isFinal());
        stageRepository.save(stage);
        auditService.log(actor.id(), "pipelineStage", stage.getId(), "create", null, toMap(stage));
        return toDto(stage);
    }

    @Transactional
    public StageDto update(UUID id, UpdateStageRequest request, CurrentUser actor) {
        PipelineStage stage = require(id);
        if (request.name() != null) {
            stage.setName(request.name());
        }
        if (request.normDays() != null) {
            stage.setNormDays(request.normDays());
        }
        if (request.position() != null && !request.position().equals(stage.getPosition())) {
            if (stageRepository.existsByPosition(request.position())) {
                throw new ConflictException("Stage position already exists");
            }
            stage.setPosition(request.position());
        }
        if (request.isActive() != null) {
            stage.setActive(request.isActive());
        }
        stageRepository.save(stage);
        auditService.log(actor.id(), "pipelineStage", id, "update", null, toMap(stage));
        return toDto(stage);
    }

    @Transactional
    public void reorder(ReorderStagesRequest request, CurrentUser actor) {
        List<UUID> ids = request.ids();
        if (ids == null || ids.size() != stageRepository.findAllActiveOrdered().size()) {
            throw new ConflictException("Reorder list must contain all active stage ids");
        }
        for (int i = 0; i < ids.size(); i++) {
            PipelineStage stage = require(ids.get(i));
            stage.setPosition(i);
            stageRepository.save(stage);
        }
        auditService.log(actor.id(), "pipelineStage", UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "reorder", null, Map.of("order", ids.toString()));
    }

    @Transactional
    public void archive(UUID id, CurrentUser actor) {
        PipelineStage stage = require(id);
        stage.setActive(false);
        stageRepository.save(stage);
        auditService.log(actor.id(), "pipelineStage", id, "archive", null, toMap(stage));
    }

    public PipelineStage require(UUID id) {
        return stageRepository.findById(id)
                .filter(PipelineStage::isActive)
                .orElseThrow(() -> new NotFoundException("Stage not found"));
    }

    private StageDto toDto(PipelineStage stage) {
        return new StageDto(stage.getId(), stage.getName(), stage.getPosition(),
                stage.getNormDays(), stage.isFinal(), stage.isActive());
    }

    private Map<String, Object> toMap(PipelineStage stage) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", stage.getName());
        m.put("position", stage.getPosition());
        m.put("normDays", stage.getNormDays());
        m.put("isFinal", stage.isFinal());
        m.put("isActive", stage.isActive());
        return m;
    }
}
