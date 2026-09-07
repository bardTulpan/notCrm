package com.pipeline.crm.user;

import com.pipeline.crm.audit.AuditService;
import com.pipeline.crm.common.exception.ConflictException;
import com.pipeline.crm.common.exception.NotFoundException;
import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public List<UserDto> list() {
        return userRepository.findAllActive().stream().map(UserDtos.MAPPER).toList();
    }

    public UserDto get(UUID id) {
        return UserDtos.toDto(require(id));
    }

    @Transactional
    public UserDto create(CreateUserRequest request, CurrentUser actor) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setAvatarColor(request.avatarColor());
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        auditService.log(actor.id(), "user", user.getId(), "create", null, safe(user));
        return UserDtos.toDto(user);
    }

    @Transactional
    public UserDto update(UUID id, UpdateUserRequest request, CurrentUser actor) {
        User user = require(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.avatarColor() != null) {
            user.setAvatarColor(request.avatarColor());
        }
        userRepository.save(user);
        auditService.log(actor.id(), "user", id, "update", null, safe(user));
        return UserDtos.toDto(user);
    }

    @Transactional
    public UserDto block(UUID id, CurrentUser actor) {
        User user = require(id);
        if (user.getRole() == Role.ADMIN && isLastActiveAdmin(user.getId())) {
            throw new ConflictException("Cannot block the last active admin");
        }
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        auditService.log(actor.id(), "user", id, "block", null, safe(user));
        return UserDtos.toDto(user);
    }

    @Transactional
    public UserDto unblock(UUID id, CurrentUser actor) {
        User user = require(id);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        auditService.log(actor.id(), "user", id, "unblock", null, safe(user));
        return UserDtos.toDto(user);
    }

    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request, CurrentUser actor) {
        User user = require(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.log(actor.id(), "user", id, "reset-password", null, null);
    }

    @Transactional
    public void delete(UUID id, CurrentUser actor) {
        User user = require(id);
        if (user.getRole() == Role.ADMIN && isLastActiveAdmin(user.getId())) {
            throw new ConflictException("Cannot delete the last active admin");
        }
        if (user.getRole() == Role.CURATOR && hasStudents(user.getId())) {
            throw new ConflictException("Curator has active students; reassign them first");
        }
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
        auditService.log(actor.id(), "user", id, "delete", null, Map.of("deletedAt", Instant.now().toString()));
    }

    private boolean isLastActiveAdmin(UUID excludeId) {
        long count = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
        return count <= 1 && userRepository.findById(excludeId)
                .map(u -> u.getRole() == Role.ADMIN && u.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }

    private boolean hasStudents(UUID curatorId) {
        return studentRepository.findByCuratorId(curatorId).stream()
                .anyMatch(s -> s.getDeletedAt() == null);
    }

    private User require(UUID id) {
        return userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private java.util.Map<String, Object> safe(User user) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("username", user.getUsername());
        m.put("fullName", user.getFullName());
        m.put("role", user.getRole().name());
        m.put("status", user.getStatus().name());
        return m;
    }
}
