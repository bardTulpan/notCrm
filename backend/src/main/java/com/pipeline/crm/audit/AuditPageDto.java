package com.pipeline.crm.audit;

import java.util.List;

public record AuditPageDto(List<AuditLogDto> items, long total, int page, int size) {
}
