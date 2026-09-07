package com.pipeline.crm.pipeline;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PipelineStageAccessor {

    private final PipelineStageRepository stageRepository;

    public Optional<PipelineStage> firstActiveStage() {
        return stageRepository.findFirstByIsActiveTrueOrderByPositionAsc();
    }
}
