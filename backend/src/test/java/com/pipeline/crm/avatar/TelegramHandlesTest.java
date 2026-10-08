package com.pipeline.crm.avatar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramHandlesTest {

    @Test
    void acceptsTheSameFormsAsTheFrontend() {
        for (String raw : new String[]{"ivan_petrov", "@ivan_petrov", " @ivan_petrov ", "t.me/ivan_petrov",
                "https://t.me/ivan_petrov", "http://telegram.me/ivan_petrov/", "https://T.me/ivan_petrov?start=1"}) {
            assertThat(TelegramHandles.normalize(raw)).as(raw).contains("ivan_petrov");
        }
    }

    @Test
    void rejectsValuesThatAreNotAHandle() {
        for (String raw : new String[]{"", "  ", "@", "ab", "иван", "!!!", "https://example.com/ivan"}) {
            assertThat(TelegramHandles.normalize(raw)).as(raw).isEmpty();
        }
        assertThat(TelegramHandles.normalize(null)).isEmpty();
    }
}
