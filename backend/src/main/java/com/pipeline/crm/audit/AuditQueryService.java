package com.pipeline.crm.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.crm.cohort.Cohort;
import com.pipeline.crm.cohort.CohortRepository;
import com.pipeline.crm.lead.Lead;
import com.pipeline.crm.lead.LeadRepository;
import com.pipeline.crm.pipeline.PipelineStage;
import com.pipeline.crm.pipeline.PipelineStageRepository;
import com.pipeline.crm.student.Student;
import com.pipeline.crm.student.StudentRepository;
import com.pipeline.crm.user.User;
import com.pipeline.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read side of the audit log: turns raw rows (ids + jsonb payloads) into something an admin can read —
 * actor/entity names are resolved at read time (so old rows work too) and a Russian one-line description is built.
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final LeadRepository leadRepository;
    private final CohortRepository cohortRepository;
    private final PipelineStageRepository stageRepository;

    public AuditPageDto list(UUID actorId, String entityType, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();
        if (actorId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("actorId"), actorId));
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType));
        }

        Page<AuditLog> result = auditLogRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        List<AuditLog> rows = result.getContent();

        Names names = new Names(rows);
        List<AuditLogDto> items = rows.stream().map(r -> toDto(r, names)).toList();
        return new AuditPageDto(items, result.getTotalElements(), safePage, safeSize);
    }

    private AuditLogDto toDto(AuditLog row, Names names) {
        String actor = row.getActorId() == null ? "Система" : names.user(row.getActorId());
        String entity = names.entity(row.getEntityType(), row.getEntityId());
        JsonNode after = parse(row.getAfterData());
        return new AuditLogDto(row.getId(), row.getCreatedAt(), row.getActorId(), actor,
                row.getEntityType(), row.getEntityId(), entity, row.getAction(),
                describe(row, entity, after, names));
    }

    private String describe(AuditLog row, String entity, JsonNode after, Names names) {
        String q = "«" + entity + "»";
        return switch (row.getEntityType()) {
            case "student" -> switch (row.getAction()) {
                case "create" -> "завёл ученика " + q;
                case "move-stage" -> {
                    String to = names.stage(text(after, "toStageId") != null ? text(after, "toStageId") : legacyId(after));
                    String from = text(after, "fromStageId") != null ? names.stage(text(after, "fromStageId")) : null;
                    String base = "перенёс ученика " + q + (from != null ? ": " + from + " → " + to : " на этап " + to);
                    yield after != null && after.path("timerRestored").asBoolean(false)
                            ? base + " (вернул на прежний этап — счётчик дней сохранён)" : base;
                }
                case "reorder" -> "изменил порядок карточки " + q + " в этапе «" + names.stage(text(after, "stageId")) + "»";
                case "pause" -> "поставил ученика " + q + " на паузу";
                case "resume" -> "снял с паузы ученика " + q;
                case "assign-curator" -> {
                    // Older rows stored just the new curator id as a bare JSON string.
                    UUID to = uuid(text(after, "toCuratorId") != null ? text(after, "toCuratorId") : legacyId(after));
                    UUID from = uuid(text(after, "fromCuratorId"));
                    yield from != null
                            ? "сменил куратора ученика " + q + ": " + names.user(from) + " → " + names.user(to)
                            : "назначил куратора ученику " + q + ": " + names.user(to);
                }
                default -> row.getAction() + " ученика " + q;
            };
            case "lead" -> switch (row.getAction()) {
                case "create" -> "создал лида " + q;
                case "update" -> "изменил лида " + q;
                case "archive" -> "архивировал лида " + q;
                case "restore" -> "вернул из архива лида " + q;
                case "convert" -> "перевёл лида " + q + " в ученики";
                default -> row.getAction() + " лида " + q;
            };
            case "user" -> switch (row.getAction()) {
                case "create" -> "создал пользователя " + q;
                case "update" -> "изменил пользователя " + q;
                case "block" -> "заблокировал пользователя " + q;
                case "unblock" -> "разблокировал пользователя " + q;
                case "reset-password" -> "сбросил пароль пользователя " + q;
                case "delete" -> "удалил пользователя " + q;
                default -> row.getAction() + " пользователя " + q;
            };
            case "cohort" -> switch (row.getAction()) {
                case "create" -> "создал когорту " + q;
                case "update" -> "изменил когорту " + q;
                case "archive" -> "архивировал когорту " + q;
                case "restore" -> "вернул из архива когорту " + q;
                default -> row.getAction() + " когорты " + q;
            };
            case "pipelineStage" -> switch (row.getAction()) {
                case "create" -> "создал этап " + q;
                case "update" -> "изменил этап " + q;
                case "archive" -> "архивировал этап " + q;
                case "reorder" -> "изменил порядок этапов";
                default -> row.getAction() + " этапа " + q;
            };
            default -> row.getAction() + " " + row.getEntityType() + " " + q;
        };
    }

    private JsonNode parse(String json) {
        if (json == null) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            // Defensive: a payload stored as a JSON *string* containing JSON gets one more unwrap.
            if (node != null && node.isTextual()) {
                String inner = node.asText();
                if (inner.startsWith("{")) return objectMapper.readTree(inner);
            }
            return node;
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.isObject() || node.get(field) == null || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    /** Older move-stage rows stored just the target stage id as a bare JSON string. */
    private static String legacyId(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private static UUID uuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Batch-resolved display names for everything referenced by one page of audit rows. */
    private final class Names {
        private final Map<UUID, String> users;
        private final Map<UUID, String> students;
        private final Map<UUID, String> leads;
        private final Map<UUID, String> cohorts;
        private final Map<UUID, String> stages;

        Names(List<AuditLog> rows) {
            Set<UUID> userIds = new HashSet<>();
            Set<UUID> studentIds = new HashSet<>();
            Set<UUID> leadIds = new HashSet<>();
            Set<UUID> cohortIds = new HashSet<>();
            Set<UUID> stageIds = new HashSet<>();
            for (AuditLog r : rows) {
                if (r.getActorId() != null) userIds.add(r.getActorId());
                switch (r.getEntityType()) {
                    case "student" -> studentIds.add(r.getEntityId());
                    case "lead" -> leadIds.add(r.getEntityId());
                    case "user" -> userIds.add(r.getEntityId());
                    case "cohort" -> cohortIds.add(r.getEntityId());
                    case "pipelineStage" -> stageIds.add(r.getEntityId());
                    default -> { }
                }
                JsonNode after = parse(r.getAfterData());
                for (String f : new String[]{"fromStageId", "toStageId", "stageId"}) addUuid(stageIds, text(after, f));
                addUuid(stageIds, legacyId(after));
                for (String f : new String[]{"fromCuratorId", "toCuratorId"}) addUuid(userIds, text(after, f));
                if ("assign-curator".equals(r.getAction())) addUuid(userIds, legacyId(after));
            }
            users = load(userIds, userRepository.findAllById(userIds), User::getId, User::getFullName);
            students = load(studentIds, studentRepository.findAllById(studentIds), Student::getId, Student::getFullName);
            leads = load(leadIds, leadRepository.findAllById(leadIds), Lead::getId, Lead::getName);
            cohorts = load(cohortIds, cohortRepository.findAllById(cohortIds), Cohort::getId, Cohort::getName);
            stages = load(stageIds, stageRepository.findAllById(stageIds), PipelineStage::getId, PipelineStage::getName);
        }

        private void addUuid(Set<UUID> target, String value) {
            UUID id = uuid(value);
            if (id != null) target.add(id);
        }

        private <T> Map<UUID, String> load(Set<UUID> ids, List<T> found, Function<T, UUID> id, Function<T, String> name) {
            if (ids.isEmpty()) return new HashMap<>();
            return found.stream().collect(Collectors.toMap(id, name, (a, b) -> a));
        }

        String user(UUID id) {
            return id == null ? "—" : users.getOrDefault(id, "—");
        }

        String stage(String id) {
            UUID parsed = uuid(id);
            return parsed == null ? "—" : stages.getOrDefault(parsed, "—");
        }

        String entity(String type, UUID id) {
            Map<UUID, String> source = switch (type) {
                case "student" -> students;
                case "lead" -> leads;
                case "user" -> users;
                case "cohort" -> cohorts;
                case "pipelineStage" -> stages;
                default -> Map.of();
            };
            return source.getOrDefault(id, "—");
        }
    }
}
