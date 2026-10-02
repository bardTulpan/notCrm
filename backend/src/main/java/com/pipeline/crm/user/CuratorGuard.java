package com.pipeline.crm.user;

import com.pipeline.crm.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Rejects ids that can't hold students/leads: unknown, deleted, blocked, or not a CURATOR (e.g. an admin or a typo). */
@Component
@RequiredArgsConstructor
public class CuratorGuard {

    private final UserRepository userRepository;

    public UUID requireAssignable(UUID curatorId) {
        boolean ok = userRepository.findById(curatorId)
                .filter(u -> u.getDeletedAt() == null)
                .filter(u -> u.getRole() == Role.CURATOR)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .isPresent();
        if (!ok) {
            throw new BadRequestException("Куратор не найден или недоступен");
        }
        return curatorId;
    }
}
