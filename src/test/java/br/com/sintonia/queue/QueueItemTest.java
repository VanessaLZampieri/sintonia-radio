package br.com.sintonia.queue;

import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class QueueItemTest {

    private final Room room = new Room("QTEST123", RoomStatus.ACTIVE);
    private final Song song = new Song("qitem-song-1", "Title", null, Duration.ofSeconds(100));
    private final User user = mock(User.class);
    private final Instant addedAt = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void createsValidQueueItem() {
        QueueItem item = new QueueItem(room, song, user, addedAt, 1);

        assertThat(item.getRoom()).isSameAs(room);
        assertThat(item.getSong()).isSameAs(song);
        assertThat(item.getUser()).isSameAs(user);
        assertThat(item.getAddedAt()).isEqualTo(addedAt);
        assertThat(item.getPosition()).isEqualTo(1);
        assertThat(item.getSource()).isEqualTo(QueueItemSource.USER);
    }

    @Test
    void autoDjItemHasNullUserAndAutoDjSource() {
        QueueItem item = new QueueItem(room, song, addedAt, 1);

        assertThat(item.getUser()).isNull();
        assertThat(item.getSource()).isEqualTo(QueueItemSource.AUTO_DJ);
        assertThat(item.getStatus()).isEqualTo(QueueItemStatus.WAITING);
    }

    @Test
    void newItemStartsWithWaiting() {
        QueueItem item = new QueueItem(room, song, user, addedAt, 1);

        assertThat(item.getStatus()).isEqualTo(QueueItemStatus.WAITING);
    }

    @Test
    void statusCanBeChanged() {
        QueueItem item = new QueueItem(room, song, user, addedAt, 1);

        item.setStatus(QueueItemStatus.PLAYING);

        assertThat(item.getStatus()).isEqualTo(QueueItemStatus.PLAYING);
    }

    @Test
    void rejectsNullStatus() {
        QueueItem item = new QueueItem(room, song, user, addedAt, 1);

        assertThatThrownBy(() -> item.setStatus(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void statusEnumHasExpectedValues() {
        assertThat(QueueItemStatus.values())
                .containsExactly(
                        QueueItemStatus.WAITING,
                        QueueItemStatus.PLAYING,
                        QueueItemStatus.FINISHED,
                        QueueItemStatus.SKIPPED,
                        QueueItemStatus.ERROR);
    }

    @Test
    void rejectsNullRoom() {
        assertThatThrownBy(() -> new QueueItem(null, song, user, addedAt, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullSong() {
        assertThatThrownBy(() -> new QueueItem(room, null, user, addedAt, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullUser() {
        assertThatThrownBy(() -> new QueueItem(room, song, null, addedAt, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullAddedAt() {
        assertThatThrownBy(() -> new QueueItem(room, song, user, null, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsInvalidPosition() {
        assertThatThrownBy(() -> new QueueItem(room, song, user, addedAt, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QueueItem(room, song, user, addedAt, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
