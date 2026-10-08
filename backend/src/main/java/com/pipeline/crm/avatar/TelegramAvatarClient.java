package com.pipeline.crm.avatar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a profile photo from the public page {@code t.me/<handle>}: the page carries the photo as og:image when the
 * handle is public and the photo is visible to everyone. Unofficial — if Telegram changes the page, results turn into
 * {@link Failed}/{@link NoPhoto} and the CRM falls back to initials; nothing else depends on it.
 */
@Component
@Slf4j
public class TelegramAvatarClient {

    public sealed interface Result permits Photo, NoPhoto, Failed {
    }

    public record Photo(byte[] jpeg) implements Result {
    }

    public record NoPhoto() implements Result {
    }

    public record Failed(String reason) implements Result {
    }

    private static final Pattern OG_IMAGE = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"");
    /** Only rendered when the profile has a photo visible to everyone; otherwise og:image is Telegram's logo. */
    private static final String PHOTO_MARKER = "tgme_page_photo_image";
    private static final int MAX_PAGE_BYTES = 512 * 1024;
    private static final int MAX_IMAGE_BYTES = 3 * 1024 * 1024;

    private final AvatarProperties props;
    private final HttpClient http;
    private Instant lastRequestAt = Instant.EPOCH;

    public TelegramAvatarClient(AvatarProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Result fetch(String handle) {
        try {
            awaitTurn();
            HttpResponse<InputStream> page = get(URI.create(props.telegramBaseUrl() + "/" + handle));
            if (page.statusCode() != 200) {
                page.body().close();
                return new Failed("t.me answered " + page.statusCode());
            }
            String html = new String(readLimited(page.body(), MAX_PAGE_BYTES), StandardCharsets.UTF_8);
            Matcher og = OG_IMAGE.matcher(html);
            if (!html.contains(PHOTO_MARKER) || !og.find()) {
                return new NoPhoto();
            }
            URI imageUri = URI.create(og.group(1).replace("&amp;", "&"));
            if (!isAllowedImageHost(imageUri.getHost())) {
                return new Failed("unexpected image host " + imageUri.getHost());
            }
            HttpResponse<InputStream> image = get(imageUri);
            if (image.statusCode() != 200) {
                image.body().close();
                return new Failed("image answered " + image.statusCode());
            }
            return new Photo(AvatarImages.toSquareJpeg(readLimited(image.body(), MAX_IMAGE_BYTES), props.sizePx()));
        } catch (IOException | IllegalArgumentException e) {
            return new Failed(e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Failed("interrupted");
        }
    }

    /** Spaces requests to Telegram out, whichever thread (edit, nightly job, refresh button) asks. */
    private synchronized void awaitTurn() throws InterruptedException {
        Instant next = lastRequestAt.plus(props.requestInterval());
        long waitMs = Duration.between(Instant.now(), next).toMillis();
        if (waitMs > 0) Thread.sleep(waitMs);
        lastRequestAt = Instant.now();
    }

    private HttpResponse<InputStream> get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private boolean isAllowedImageHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        return props.allowedImageHosts().stream().anyMatch(a -> h.equals(a) || h.endsWith("." + a));
    }

    private static byte[] readLimited(InputStream in, int max) throws IOException {
        try (in) {
            byte[] bytes = in.readNBytes(max + 1);
            if (bytes.length > max) throw new IOException("response larger than " + max + " bytes");
            return bytes;
        }
    }
}
