package com.pipeline.crm.avatar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_avatars")
@Getter
@Setter
public class StudentAvatar {

    @Id
    @Column(name = "student_id")
    private UUID studentId;

    /** The student's Telegram value this row was checked for; a mismatch means the handle changed since. */
    @Column(name = "telegram_username", nullable = false, length = 100)
    private String telegramUsername;

    /** Square JPEG, or null when Telegram shows no photo. */
    @Column(name = "image")
    private byte[] image;

    /** Short content hash of {@link #image}; the frontend uses it as a cache-busting version. */
    @Column(name = "image_hash", length = 16)
    private String imageHash;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "next_check_at", nullable = false)
    private Instant nextCheckAt;
}
