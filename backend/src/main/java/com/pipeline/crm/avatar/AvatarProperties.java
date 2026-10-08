package com.pipeline.crm.avatar;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Students' Telegram photos. Every check costs one request to t.me plus one to its CDN; requests are spaced by
 * {@code requestInterval} and the nightly job only re-checks rows whose {@code next_check_at} has passed.
 */
@ConfigurationProperties(prefix = "app.avatars")
public record AvatarProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("https://t.me") String telegramBaseUrl,
        /* og:image must point at one of these hosts (or a subdomain) — we never follow an arbitrary URL. */
        @DefaultValue("telesco.pe") List<String> allowedImageHosts,
        @DefaultValue("14d") Duration refreshInterval,
        /* Random extra delay per student, so the whole roster doesn't come due on the same night. */
        @DefaultValue("3d") Duration refreshJitter,
        @DefaultValue("1d") Duration retryAfterFailure,
        /* Telegram intermittently serves the page without the photo, more often under a burst of requests. */
        @DefaultValue("5s") Duration requestInterval,
        /* So "no photo" is only believed after this many answers in a row (a photo in any of them wins). */
        @DefaultValue("3") int noPhotoAttempts,
        @DefaultValue("1m") Duration manualRefreshCooldown,
        @DefaultValue("1m") Duration startupDelay,
        @DefaultValue("200") int batchLimit,
        @DefaultValue("128") int sizePx,
        /* dev profile only: a folder of JPEGs handed out to demo students (see DevAvatarSeeder). */
        @DefaultValue("") String devSeedDir) {
}
