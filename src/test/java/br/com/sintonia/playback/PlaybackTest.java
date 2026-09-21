package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void pauseMarksPaused() {
        Playback playback = new Playback(queueItem, startedAt);

        playback.pause();

        assertThat(playback.isPaused()).isTrue();
        assertThat(playback.getPausedAt()).isNotNull();
    }

    @Test
    void pauseIsIdempotent() {
        Playback playback = new Playback(queueItem, startedAt);
        playback.pause();
        Instant first = playback.getPausedAt();

        playback.pause();

        assertThat(playback.getPausedAt()).isEqualTo(first);
    }

    @Test
    void resumeClearsPaused() {
        Playback playback = new Playback(queueItem, startedAt);
        playback.pause();

        playback.resume();

        assertThat(playback.isPaused()).isFalse();
        assertThat(playback.getPausedAt()).isNull();
    }

    @Test
    void positionAdvancesWhilePlaying() {
        Playback playback = new Playback(queueItem, startedAt);

        long position = playback.positionSeconds(Instant.parse("2026-01-01T19:30:30Z"));

        assertThat(position).isEqualTo(30);
    }

    @Test
    void positionFreezesWhilePaused() {
        Playback playback = new Playback(queueItem, startedAt);
        ReflectionTestUtils.setField(playback, "pausedAt", Instant.parse("2026-01-01T19:30:20Z"));

        long position = playback.positionSeconds(Instant.parse("2026-01-01T19:31:00Z"));

        assertThat(position).isEqualTo(20);
    }

    @Test
    void pausedTimeIsExcludedFromPosition() {
        Playback playback = new Playback(queueItem, startedAt);
        ReflectionTestUtils.setField(playback, "totalPausedMillis", 10_000L);

        long position = playback.positionSeconds(Instant.parse("2026-01-01T19:30:40Z"));

        assertThat(position).isEqualTo(30);
    }

    @Test
    void positionIsNeverNegative() {
        Playback playback = new Playback(queueItem, startedAt);
        ReflectionTestUtils.setField(playback, "totalPausedMillis", 10_000L);

        long position = playback.positionSeconds(Instant.parse("2026-01-01T19:30:05Z"));

        assertThat(position).isEqualTo(0);
    }
}