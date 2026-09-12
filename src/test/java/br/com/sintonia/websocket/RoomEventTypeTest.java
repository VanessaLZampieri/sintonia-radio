package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomEventTypeTest {

    @Test
    void hasExpectedValues() {
        assertThat(RoomEventType.values()).containsExactly(
                RoomEventType.PLAYBACK_STARTED,
                RoomEventType.PLAYBACK_FINISHED,
                RoomEventType.PLAYBACK_SKIPPED,
                RoomEventType.QUEUE_CHANGED);
    }
}