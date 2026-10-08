package com.pipeline.crm.avatar;

import java.util.Optional;
import java.util.regex.Pattern;

/** Same rules as the frontend's utils/telegram.ts: "@name", "name", "t.me/name" and "https://t.me/name" all work. */
final class TelegramHandles {

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{3,64}");

    private TelegramHandles() {
    }

    static Optional<String> normalize(String raw) {
        if (raw == null) return Optional.empty();
        String handle = raw.trim()
                .replaceFirst("(?i)^https?://", "")
                .replaceFirst("(?i)^(www\\.)?(t|telegram)\\.me/", "")
                .replaceFirst("^@", "")
                .split("[/?#\\s]", 2)[0];
        return VALID.matcher(handle).matches() ? Optional.of(handle) : Optional.empty();
    }
}
