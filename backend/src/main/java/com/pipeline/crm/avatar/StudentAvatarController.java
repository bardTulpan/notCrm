package com.pipeline.crm.avatar;

import com.pipeline.crm.common.exception.NotFoundException;
import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.security.SecurityUtils;
import com.pipeline.crm.student.StudentDto;
import com.pipeline.crm.student.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

/** Same visibility as the student itself: another curator's student is a 404. */
@RestController
@RequestMapping("/api/v1/students/{id}/avatar")
@RequiredArgsConstructor
public class StudentAvatarController {

    private final StudentService studentService;
    private final StudentAvatarService avatarService;
    private final SecurityUtils securityUtils;

    /** The frontend asks with ?v=<avatarVersion>, so a long private cache is safe: a new photo is a new URL. */
    @GetMapping
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        studentService.requireAccessible(id, actor());
        StudentAvatarService.Photo photo = avatarService.photo(id)
                .orElseThrow(() -> new NotFoundException("No photo"));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate())
                .eTag(photo.hash())
                .body(photo.image());
    }

    @PostMapping("/refresh")
    public StudentDto refresh(@PathVariable UUID id) {
        studentService.requireAccessible(id, actor());
        avatarService.refreshNow(id);
        return studentService.get(id, actor());
    }

    private CurrentUser actor() {
        return securityUtils.currentUser();
    }
}
