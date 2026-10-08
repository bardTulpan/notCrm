package com.pipeline.crm.avatar;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramAvatarClientTest {

    private static final TelegramStub STUB = new TelegramStub();

    private static TelegramAvatarClient client(Duration requestInterval) {
        return new TelegramAvatarClient(new AvatarProperties(true, STUB.baseUrl(), List.of("127.0.0.1"),
                Duration.ofDays(14), Duration.ofDays(3), Duration.ofDays(1), requestInterval,
                Duration.ofMinutes(1), Duration.ofMinutes(1), 200, 128, ""));
    }

    @AfterAll
    static void stopStub() {
        STUB.stop();
    }

    @Test
    void photoIsCroppedToASmallSquareJpeg() throws Exception {
        STUB.photo("client_photo", Color.RED);
        var result = client(Duration.ZERO).fetch("client_photo");

        assertThat(result).isInstanceOf(TelegramAvatarClient.Photo.class);
        byte[] jpeg = ((TelegramAvatarClient.Photo) result).jpeg();
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertThat(img.getWidth()).isEqualTo(128);
        assertThat(img.getHeight()).isEqualTo(128);
        assertThat(jpeg.length).isLessThan(10_000);
    }

    @Test
    void pageWithoutAPublicPhotoMeansNoPhoto() {
        STUB.set("client_hidden", TelegramStub.Mode.NO_PHOTO);
        assertThat(client(Duration.ZERO).fetch("client_hidden")).isInstanceOf(TelegramAvatarClient.NoPhoto.class);
    }

    @Test
    void serverErrorIsAFailureNotAMissingPhoto() {
        STUB.set("client_error", TelegramStub.Mode.ERROR);
        assertThat(client(Duration.ZERO).fetch("client_error")).isInstanceOf(TelegramAvatarClient.Failed.class);
    }

    @Test
    void imageOnAHostOutsideTheAllowListIsNeverDownloaded() {
        STUB.set("client_foreign", TelegramStub.Mode.FOREIGN_HOST);
        var result = client(Duration.ZERO).fetch("client_foreign");
        assertThat(result).isInstanceOf(TelegramAvatarClient.Failed.class);
        assertThat(((TelegramAvatarClient.Failed) result).reason()).contains("example.com");
    }

    @Test
    void requestsAreSpacedOut() {
        TelegramAvatarClient client = client(Duration.ofMillis(300));
        long start = System.nanoTime();
        client.fetch("client_spacing_a");
        client.fetch("client_spacing_b");
        client.fetch("client_spacing_c");
        // the first request goes out at once, each next one waits for the interval
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(600));
    }
}
