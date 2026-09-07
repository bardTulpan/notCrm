package com.pipeline.crm.student;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_curator_history")
@Getter
@Setter
public class StudentCuratorHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "from_curator_id")
    private UUID fromCuratorId;

    @Column(name = "to_curator_id", nullable = false)
    private UUID toCuratorId;

    @Column(name = "changed_by_id", nullable = false)
    private UUID changedById;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
