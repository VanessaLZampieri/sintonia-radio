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
                RoomEventType.PLAYBACK_ERROR,
                RoomEventType.QUEUE_CHANGED,
                RoomEventType.PLAYER_CHANGED,
                RoomEventType.PLAYBACK_MODE_CHANGED,
                RoomEventType.SKIP_VOTE_CHANGED);
    }
}