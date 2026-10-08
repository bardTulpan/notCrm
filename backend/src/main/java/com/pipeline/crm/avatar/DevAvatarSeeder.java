package com.pipeline.crm.avatar;

import com.pipeline.crm.student.Student;
import com.pipeline.crm.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Local dev only: gives demo students photos from a folder of JPEGs (app.avatars.dev-seed-dir) so the board looks
 * like production. Runs once — only while no student has a photo row yet. The photos are kept outside the repo.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAvatarSeeder {

    private static final String SEED_HANDLE = "dev-seed";

    private final AvatarProperties props;
    private final StudentRepository studentRepository;
    private final StudentAvatarRepository avatarRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() throws IOException {
        if (props.devSeedDir().isBlank() || avatarRepository.count() > 0) return;
        Path dir = Path.of(props.devSeedDir());
        if (!Files.isDirectory(dir)) {
            log.info("Dev avatars: folder {} not found, demo students keep initials", dir.toAbsolutePath());
            return;
        }
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jpg")).sorted().toList();
        }
        // Roughly the real share (about 7 in 10 students have a public photo), in a stable order.
        List<Student> students = studentRepository.findAll().stream()
                .filter(st -> st.getDeletedAt() == null)
                .sorted(Comparator.comparing(st -> st.getId().toString()))
                .filter(st -> Math.floorMod(st.getId().hashCode(), 10) < 7)
                .limit(files.size())
                .toList();
        Instant farFuture = Instant.now().plus(3650, ChronoUnit.DAYS);
        for (int i = 0; i < students.size(); i++) {
            byte[] image = Files.readAllBytes(files.get(i));
            StudentAvatar avatar = new StudentAvatar();
            avatar.setStudentId(students.get(i).getId());
            avatar.setTelegramUsername(SEED_HANDLE);
            avatar.setImage(image);
            avatar.setImageHash(AvatarImages.hash(image));
            avatar.setCheckedAt(Instant.now());
            avatar.setNextCheckAt(farFuture);
            avatarRepository.save(avatar);
        }
        log.info("Dev avatars: gave {} demo student(s) a photo from {}", students.size(), dir.toAbsolutePath());
    }
}
