package com.pipeline.crm.lead;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LeadSpecifications {

    private LeadSpecifications() {
    }

    public static Specification<Lead> filter(LeadStatus status, String search, UUID assignedCuratorId,
                                               Instant pingFrom, Instant pingTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (assignedCuratorId != null) {
                predicates.add(cb.equal(root.get("assignedCuratorId"), assignedCuratorId));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("telegramUsername")), like)));
            }
            if (pingFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("nextPingAt"), pingFrom));
            }
            if (pingTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("nextPingAt"), pingTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
