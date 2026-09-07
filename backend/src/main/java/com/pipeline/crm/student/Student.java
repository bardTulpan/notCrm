package com.pipeline.crm.student;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "students")
@Getter
@Setter
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "source_lead_id", unique = true)
    private UUID sourceLeadId;

    @Column(name = "current_stage_id", nullable = false)
    private UUID currentStageId;

    @Column(name = "curator_id", nullable = false)
    private UUID curatorId;

    @Column(name = "cohort_id")
    private UUID cohortId;

    @Column(name = "stage_entered_at", nullable = false)
    private Instant stageEnteredAt;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "is_paused", nullable = false)
    private boolean isPaused = false;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "postpay_percent")
    private Integer postpayPercent;

    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<StudentNote> notes = new ArrayList<>();
}
