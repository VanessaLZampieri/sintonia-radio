package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaybackEventPayloadTest {

    @Test
    void createsValidPayload() {
        PlaybackEventPayload payload = new PlaybackEventPayload(10L, 42L);

        assertThat(payload.playbackId()).isEqualTo(10L);
        assertThat(payload.queueItemId()).isEqualTo(42L);
    }

    @Test
    void rejectsNullPlaybackId() {
        assertThatThrownBy(() -> new PlaybackEventPayload(null, 42L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullQueueItemId() {
        assertThatThrownBy(() -> new PlaybackEventPayload(10L, null))
                .isInstanceOf(NullPointerException.class);
    }
}