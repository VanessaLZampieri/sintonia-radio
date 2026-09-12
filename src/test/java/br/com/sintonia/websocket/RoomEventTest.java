package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomEventTest {

    @Test
    void createsValidRoomEvent() {
        RoomEvent event = new RoomEvent(RoomEventType.PLAYBACK_STARTED, 1L, Map.of("playbackId", 10L));

        assertThat(event.eventType()).isEqualTo(RoomEventType.PLAYBACK_STARTED);
        assertThat(event.roomId()).isEqualTo(1L);
        assertThat(event.payload()).isEqualTo(Map.of("playbackId", 10L));
    }

    @Test
    void rejectsNullEventType() {
        assertThatThrownBy(() -> new RoomEvent(null, 1L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullRoomId() {
        assertThatThrownBy(() -> new RoomEvent(RoomEventType.QUEUE_CHANGED, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void allowsNullPayload() {
        RoomEvent event = new RoomEvent(RoomEventType.PLAYBACK_FINISHED, 1L, null);

        assertThat(event.payload()).isNull();
    }
}