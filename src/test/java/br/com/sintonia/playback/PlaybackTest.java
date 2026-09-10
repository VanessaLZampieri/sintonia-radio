package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PlaybackTest {

    private final QueueItem queueItem = mock(QueueItem.class);
    private final Instant startedAt = Instant.parse("2026-01-01T19:30:00Z");

    @Test
    void createsValidPlayback() {
        Playback playback = new Playback(queueItem, startedAt);

        assertThat(playback.getQueueItem()).isSameAs(queueItem);
        assertThat(playback.getStartedAt()).isEqualTo(startedAt);
    }

    @Test
    void startsWithPlaying() {
        Playback playback = new Playback(queueItem, startedAt);

        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.PLAYING);
    }

    @Test
    void startsWithNullEndedAt() {
        Playback playback = new Playback(queueItem, startedAt);

        assertThat(playback.getEndedAt()).isNull();
    }

    @Test
    void rejectsNullQueueItem() {
        assertThatThrownBy(() -> new Playback(null, startedAt))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullStartedAt() {
        assertThatThrownBy(() -> new Playback(queueItem, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void returnsQueueItem() {
        Playback playback = new Playback(queueItem, startedAt);

        assertThat(playback.getQueueItem()).isSameAs(queueItem);
    }

    @Test
    void returnsStartedAt() {
        Playback playback = new Playback(queueItem, startedAt);

        assertThat(playback.getStartedAt()).isEqualTo(startedAt);
    }

    @Test
    void returnsStatus() {
        Playback playback = new Playback(queueItem, startedAt);

        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.PLAYING);
    }

    @Test
    void returnsNullEndedAt() {
        Playback playback = new Playback(queueItem, startedAt);

        assertThat(playback.getEndedAt()).isNull();
    }
}