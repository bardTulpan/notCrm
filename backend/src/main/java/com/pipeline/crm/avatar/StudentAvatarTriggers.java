package com.pipeline.crm.avatar;

import com.pipeline.crm.student.StudentTelegramChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class StudentAvatarTriggers {

    private final StudentAvatarService avatarService;
    private final AvatarProperties props;
    private final TaskScheduler taskScheduler;

    /** A new student or a changed handle: fetch in the background once the change is committed. */
    @Async("avatarExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTelegramChanged(StudentTelegramChanged event) {
        avatarService.refresh(event.studentId());
    }

    /** 01:00 UTC, clear of the 03:00 UTC database backup. */
    @Scheduled(cron = "${app.avatars.refresh-cron:0 0 1 * * *}", zone = "UTC")
    public void nightly() {
        avatarService.refreshDue();
    }

    /** Also shortly after startup, so a deploy (or the very first one) doesn't wait for the night. */
    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup() {
        if (props.enabled()) {
            taskScheduler.schedule(avatarService::refreshDue, Instant.now().plus(props.startupDelay()));
        }
    }
}
