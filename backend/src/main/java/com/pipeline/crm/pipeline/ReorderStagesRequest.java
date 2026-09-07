package com.pipeline.crm.pipeline;

import java.util.List;
import java.util.UUID;

public record ReorderStagesRequest(List<UUID> ids) {
}
