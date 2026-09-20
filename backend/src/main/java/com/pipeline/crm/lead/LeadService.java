package com.pipeline.crm.lead;

import com.pipeline.crm.audit.AuditService;
import com.pipeline.crm.common.exception.ConflictException;
import com.pipeline.crm.common.exception.NotFoundException;
import com.pipeline.crm.pipeline.PipelineStageAccessor;
import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.student.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final PipelineStageAccessor stageAccessor;
    private final StudentRepository studentRepository;
    private final StudentStageHistoryRepository stageHistoryRepository;
    private final AuditService auditService;

    public List<LeadDto> list(LeadStatus status, String search, UUID assignedCuratorId,
                               Instant pingFrom, Instant pingTo, CurrentUser user) {
        Specification<Lead> spec = LeadSpecifications.filter(status, search, assignedCuratorId, pingFrom, pingTo);
        if (!user.isAdmin()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedCuratorId"), user.id()));
        }
        return leadRepository.findAll(spec, Sort.by(Sort.Order.desc("createdAt"))).stream().map(this::toDto).toList();
    }

    public LeadDto get(UUID id, CurrentUser user) {
        Lead lead = requireFor(id, user);
        return toDto(lead);
    }

    @Transactional
    public LeadDto create(CreateLeadRequest request, CurrentUser user) {
        Lead lead = new Lead();
        lead.setName(request.name());
        lead.setTelegramUsername(request.telegramUsername());
        lead.setPriceDescription(request.priceDescription());
        lead.setPostpayPercent(request.postpayPercent());
        lead.setNextPingAt(request.nextPingAt());
        lead.setStatus(LeadStatus.ACTIVE);
        lead.setCreatedById(user.id());
        lead.setAssignedCuratorId(user.isAdmin() ? request.curatorId() : user.id());
        applyNotes(lead, request.notes());
        leadRepository.save(lead);
        auditService.log(user.id(), "lead", lead.getId(), "create", null, toMap(lead));
        return toDto(lead);
    }

    @Transactional
    public LeadDto update(UUID id, UpdateLeadRequest request, CurrentUser user) {
        Lead lead = requireFor(id, user);
        if (request.name() != null) lead.setName(request.name());
        if (request.telegramUsername() != null) lead.setTelegramUsername(request.telegramUsername());
        if (request.priceDescription() != null) lead.setPriceDescription(request.priceDescription());
        if (request.postpayPercent() != null) lead.setPostpayPercent(request.postpayPercent());
        if (request.nextPingAt() != null) lead.setNextPingAt(request.nextPingAt());
        if (request.curatorId() != null) {
            if (!user.isAdmin()) {
                lead.setAssignedCuratorId(user.id());
            } else {
                lead.setAssignedCuratorId(request.curatorId());
            }
        }
        if (request.notes() != null) {
            lead.getNotes().clear();
            applyUpdateNotes(lead, request.notes());
        }
        leadRepository.save(lead);
        auditService.log(user.id(), "lead", id, "update", null, toMap(lead));
        return toDto(lead);
    }

    @Transactional
    public void archive(UUID id, CurrentUser user) {
        Lead lead = requireFor(id, user);
        lead.setStatus(LeadStatus.ARCHIVED);
        lead.setArchivedAt(Instant.now());
        lead.setArchivedById(user.id());
        leadRepository.save(lead);
        auditService.log(user.id(), "lead", id, "archive", null, toMap(lead));
    }

    @Transactional
    public void restore(UUID id, CurrentUser user) {
        Lead lead = requireFor(id, user);
        lead.setStatus(LeadStatus.ACTIVE);
        lead.setArchivedAt(null);
        lead.setArchivedById(null);
        lead.setNextPingAt(Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS));
        leadRepository.save(lead);
        auditService.log(user.id(), "lead", id, "restore", null, toMap(lead));
    }

    @Transactional
    public LeadDto postponePing(UUID id, PostponePingRequest request, CurrentUser user) {
        Lead lead = requireFor(id, user);
        Instant startOfToday = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        if (request.nextPingAt().isBefore(startOfToday)) {
            throw new ConflictException("Ping date cannot be earlier than today");
        }
        lead.setNextPingAt(request.nextPingAt());
        leadRepository.save(lead);
        return toDto(lead);
    }

    @Transactional
    public StudentDto convert(UUID id, ConvertLeadRequest request, CurrentUser user) {
        Lead lead = requireFor(id, user);
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new ConflictException("Lead already converted");
        }

        if (user.isAdmin() && request.curatorId() == null) {
            throw new com.pipeline.crm.common.exception.BadRequestException("curatorId is required");
        }
        UUID curatorId = user.isAdmin() ? request.curatorId() : user.id();

        var firstStage = stageAccessor.firstActiveStage()
                .orElseThrow(() -> new ConflictException("No active stage available"));

        Student student = new Student();
        student.setFullName(lead.getName());
        student.setSourceLeadId(lead.getId());
        student.setCurrentStageId(firstStage.getId());
        student.setCuratorId(curatorId);
        student.setCohortId(request.cohortId());
        student.setStageEnteredAt(request.startedAt() != null ? request.startedAt() : Instant.now());
        student.setStartedAt(request.startedAt() != null ? request.startedAt() : Instant.now());
        student.setPostpayPercent(request.postpayPercent() != null ? request.postpayPercent() : lead.getPostpayPercent());
        student.setCreatedById(user.id());
        if (lead.getNotes() != null) {
            int pos = 0;
            for (LeadNote note : lead.getNotes()) {
                StudentNote sn = new StudentNote();
                sn.setStudent(student);
                sn.setText(note.getText());
                sn.setPosition(note.getPosition() != null ? note.getPosition() : pos++);
                student.getNotes().add(sn);
            }
        }
        studentRepository.save(student);

        StudentStageHistory history = new StudentStageHistory();
        history.setStudentId(student.getId());
        history.setStageId(firstStage.getId());
        history.setEnteredAt(student.getStageEnteredAt());
        history.setChangedById(user.id());
        stageHistoryRepository.save(history);

        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedStudentId(student.getId());
        leadRepository.save(lead);

        auditService.log(user.id(), "lead", id, "convert", null,
                java.util.Map.of("studentId", student.getId().toString()));

        return StudentDtos.toDto(student, firstStage.getNormDays());
    }

    private Lead requireFor(UUID id, CurrentUser user) {
        Lead lead = leadRepository.findById(id)
                .filter(l -> l.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Lead not found"));
        if (!user.isAdmin() && !user.id().equals(lead.getAssignedCuratorId())) {
            throw new NotFoundException("Lead not found");
        }
        return lead;
    }

    private void applyNotes(Lead lead, List<CreateLeadRequest.NoteDto> notes) {
        if (notes == null) return;
        int pos = 0;
        for (CreateLeadRequest.NoteDto note : notes) {
            LeadNote ln = new LeadNote();
            ln.setLead(lead);
            ln.setText(note.text());
            ln.setPosition(note.position() != null ? note.position() : pos++);
            lead.getNotes().add(ln);
        }
    }

    private void applyUpdateNotes(Lead lead, List<UpdateLeadRequest.NoteDto> notes) {
        if (notes == null) return;
        int pos = 0;
        for (UpdateLeadRequest.NoteDto note : notes) {
            LeadNote ln = new LeadNote();
            ln.setLead(lead);
            ln.setText(note.text());
            ln.setPosition(note.position() != null ? note.position() : pos++);
            lead.getNotes().add(ln);
        }
    }

    private LeadDto toDto(Lead lead) {
        List<LeadDto.NoteDto> noteDtos = lead.getNotes().stream()
                .map(n -> new LeadDto.NoteDto(n.getId(), n.getText(), n.getPosition()))
                .toList();
        return new LeadDto(
                lead.getId(),
                lead.getName(),
                lead.getTelegramUsername(),
                lead.getPriceDescription(),
                lead.getPostpayPercent(),
                lead.getNextPingAt(),
                lead.getStatus(),
                lead.getAssignedCuratorId(),
                lead.getCreatedById(),
                lead.getConvertedStudentId(),
                lead.getArchivedAt(),
                noteDtos);
    }

    private java.util.Map<String, Object> toMap(Lead lead) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("name", lead.getName());
        m.put("status", lead.getStatus().name());
        m.put("assignedCuratorId", lead.getAssignedCuratorId());
        return m;
    }
}
