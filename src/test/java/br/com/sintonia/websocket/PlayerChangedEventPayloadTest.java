package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerChangedEventPayloadTest {

    @Test
    void representsAssumedPlayer() {
        PlayerChangedEventPayload payload = new PlayerChangedEventPayload("session-1", 100L);

        assertThat(payload.clientSessionId()).isEqualTo("session-1");
        assertThat(payload.userId()).isEqualTo(100L);
    }

    @Test
    void representsReleasedPlayer() {
        PlayerChangedEventPayload payload = new PlayerChangedEventPayload(null, null);

        assertThat(payload.clientSessionId()).isNull();
        assertThat(payload.userId()).isNull();
    }
}
