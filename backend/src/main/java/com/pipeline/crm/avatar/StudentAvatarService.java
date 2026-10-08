package com.pipeline.crm.avatar;

import com.pipeline.crm.common.exception.TooManyRequestsException;
import com.pipeline.crm.student.Student;
import com.pipeline.crm.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentAvatarService {

    public record Photo(byte[] image, String hash) {
    }

    private final StudentRepository studentRepository;
    private final StudentAvatarRepository avatarRepository;
    private final TelegramAvatarClient telegram;
    private final AvatarProperties props;
    private final Map<UUID, Instant> manualRefreshes = new ConcurrentHashMap<>();

    public Optional<Photo> photo(UUID studentId) {
        return avatarRepository.findById(studentId)
                .filter(a -> a.getImage() != null)
                .map(a -> new Photo(a.getImage(), a.getImageHash()));
    }

    /**
     * Re-checks one student's photo. Synchronized so concurrent triggers (an edit, the nightly job, the refresh
     * button) never race on the same row; the network call happens outside any DB transaction.
     */
    public synchronized void refresh(UUID studentId) {
        if (!props.enabled()) return;
        Student student = studentRepository.findById(studentId).filter(s -> s.getDeletedAt() == null).orElse(null);
        String raw = student != null ? student.getTelegramUsername() : null;
        if (raw == null) {
            // No handle (or the student is gone): don't keep a photo we can no longer vouch for.
            avatarRepository.deleteById(studentId);
            return;
        }

        Instant now = Instant.now();
        StudentAvatar avatar = avatarRepository.findById(studentId).orElseGet(StudentAvatar::new);
        avatar.setStudentId(studentId);
        avatar.setTelegramUsername(raw);
        avatar.setCheckedAt(now);

        TelegramAvatarClient.Result result = TelegramHandles.normalize(raw)
                .map(telegram::fetch)
                .orElseGet(TelegramAvatarClient.NoPhoto::new);
        switch (result) {
            case TelegramAvatarClient.Photo p -> {
                avatar.setImage(p.jpeg());
                avatar.setImageHash(AvatarImages.hash(p.jpeg()));
                avatar.setNextCheckAt(nextRegularCheck(now));
            }
            case TelegramAvatarClient.NoPhoto n -> {
                // Hidden or removed in Telegram: drop our copy too.
                avatar.setImage(null);
                avatar.setImageHash(null);
                avatar.setNextCheckAt(nextRegularCheck(now));
            }
            case TelegramAvatarClient.Failed f -> {
                // Keep the last known photo and try again soon.
                log.warn("Telegram photo check failed for student {}: {}", studentId, f.reason());
                avatar.setNextCheckAt(now.plus(props.retryAfterFailure()));
            }
        }
        avatarRepository.save(avatar);
    }

    /** The "refresh photo" button: at most once per cooldown per student, so it can't be used to hammer Telegram. */
    public void refreshNow(UUID studentId) {
        Instant now = Instant.now();
        boolean[] allowed = {false};
        manualRefreshes.compute(studentId, (id, last) -> {
            if (last != null && last.plus(props.manualRefreshCooldown()).isAfter(now)) return last;
            allowed[0] = true;
            return now;
        });
        if (!allowed[0]) {
            throw new TooManyRequestsException("Фото можно обновлять не чаще раза в минуту");
        }
        refresh(studentId);
    }

    /** Nightly (and shortly after startup): everyone never checked, due, or with a changed handle. */
    public void refreshDue() {
        if (!props.enabled()) return;
        List<UUID> due = avatarRepository.findDue(Instant.now(), props.batchLimit());
        if (due.isEmpty()) return;
        log.info("Checking Telegram photos of {} student(s)", due.size());
        for (UUID id : due) {
            try {
                refresh(id);
            } catch (RuntimeException e) {
                log.warn("Telegram photo check crashed for student {}", id, e);
            }
        }
    }

    private Instant nextRegularCheck(Instant now) {
        long jitterMs = props.refreshJitter().toMillis();
        long extra = jitterMs > 0 ? ThreadLocalRandom.current().nextLong(jitterMs) : 0;
        return now.plus(props.refreshInterval()).plus(Duration.ofMillis(extra));
    }
}
