package com.pipeline.crm.lead;

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
@Table(name = "leads")
@Getter
@Setter
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "telegram_username", length = 100)
    private String telegramUsername;

    @Column(name = "price_description", length = 255)
    private String priceDescription;

    @Column(name = "postpay_percent")
    private Integer postpayPercent;

    @Column(name = "next_ping_at")
    private Instant nextPingAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadStatus status;

    @Column(name = "assigned_curator_id")
    private UUID assignedCuratorId;

    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    @Column(name = "converted_student_id")
    private UUID convertedStudentId;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by_id")
    private UUID archivedById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LeadNote> notes = new ArrayList<>();
}
