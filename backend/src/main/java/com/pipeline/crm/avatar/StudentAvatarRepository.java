package com.pipeline.crm.avatar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StudentAvatarRepository extends JpaRepository<StudentAvatar, UUID> {

    /** Photo versions only — never loads the image bytes. */
    @Query("select new com.pipeline.crm.avatar.AvatarVersion(a.studentId, a.imageHash) from StudentAvatar a "
            + "where a.studentId in :ids and a.imageHash is not null")
    List<AvatarVersion> findVersions(@Param("ids") Collection<UUID> ids);

    /** Live students with a Telegram handle that were never checked, are due, or changed their handle since. */
    @Query(value = """
            SELECT s.id FROM students s
            LEFT JOIN student_avatars a ON a.student_id = s.id
            WHERE s.deleted_at IS NULL AND s.telegram_username IS NOT NULL
              AND (a.student_id IS NULL OR a.next_check_at <= :now OR a.telegram_username <> s.telegram_username)
            ORDER BY a.next_check_at NULLS FIRST
            LIMIT :limit""", nativeQuery = true)
    List<UUID> findDue(@Param("now") Instant now, @Param("limit") int limit);
}
