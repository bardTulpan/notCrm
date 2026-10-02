package com.pipeline.crm.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Names-only staff directory for any signed-in user. Lets a curator see who wrote a comment or who the previous
 * curator was, and pick a colleague when they have the "reassign" permission — without exposing the admin-only
 * user list (usernames, last login, permissions).
 */
@RestController
@RequestMapping("/api/v1/directory/users")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final UserRepository userRepository;

    public record Entry(UUID id, String fullName, String avatarColor, Role role, boolean blocked) {
    }

    @GetMapping
    public List<Entry> list() {
        return userRepository.findAllActive().stream()
                .map(u -> new Entry(u.getId(), u.getFullName(), u.getAvatarColor(), u.getRole(), u.getStatus() == UserStatus.BLOCKED))
                .toList();
    }
}
