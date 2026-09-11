package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PlaybackRepositoryTest {

    @Autowired
    private PlaybackRepository playbackRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findByQueueItemRoomIdAndStatus_returnsPlayingPlayback() {
        Room room = persistRoom("PT000001");
        Song song = persistSong("song-1");
        User user = persistUser("g1", "e1@example.com");
        QueueItem queueItem = persistQueueItem(room, song, user, 1);
        Playback playback = persistPlayback(queueItem, Instant.now());

        Optional<Playback> result =
                playbackRepository.findByQueueItemRoomIdAndStatus(room.getId(), PlaybackStatus.PLAYING);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(playback.getId());
    }

    @Test
    void findByQueueItemRoomIdAndStatus_returnsEmptyWhenNoPlayingPlayback() {
        Room room = persistRoom("PT000002");
        Song song = persistSong("song-2");
        User user = persistUser("g2", "e2@example.com");
        QueueItem queueItem = persistQueueItem(room, song, user, 1);
        persistPlayback(queueItem, Instant.now(), PlaybackStatus.FINISHED);

        Optional<Playback> result =
                playbackRepository.findByQueueItemRoomIdAndStatus(room.getId(), PlaybackStatus.PLAYING);

        assertThat(result).isEmpty();
    }

    @Test
    void findByQueueItemRoomIdAndStatus_doesNotMixRooms() {
        Room roomA = persistRoom("PT000003");
        Room roomB = persistRoom("PT000004");
        Song songA = persistSong("song-3a");
        Song songB = persistSong("song-3b");
        User user = persistUser("g3", "e3@example.com");
        QueueItem queueItemA = persistQueueItem(roomA, songA, user, 1);
        QueueItem queueItemB = persistQueueItem(roomB, songB, user, 1);
        Playback playbackA = persistPlayback(queueItemA, Instant.now());
        persistPlayback(queueItemB, Instant.now());

        Optional<Playback> result =
                playbackRepository.findByQueueItemRoomIdAndStatus(roomA.getId(), PlaybackStatus.PLAYING);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(playbackA.getId());
    }

    @Test
    void findByQueueItemId_returnsPlaybacksOfQueueItem() {
        Room room = persistRoom("PT000005");
        Song song = persistSong("song-5");
        User user = persistUser("g5", "e5@example.com");
        QueueItem queueItem = persistQueueItem(room, song, user, 1);
        persistPlayback(queueItem, Instant.now());
        persistPlayback(queueItem, Instant.now());

        List<Playback> result = playbackRepository.findByQueueItemId(queueItem.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findByQueueItemId_doesNotReturnOtherQueueItems() {
        Room room = persistRoom("PT000006");
        Song songA = persistSong("song-6a");
        Song songB = persistSong("song-6b");
        User user = persistUser("g6", "e6@example.com");
        QueueItem queueItemA = persistQueueItem(room, songA, user, 1);
        QueueItem queueItemB = persistQueueItem(room, songB, user, 2);
        Playback playbackA = persistPlayback(queueItemA, Instant.now());
        persistPlayback(queueItemB, Instant.now());

        List<Playback> result = playbackRepository.findByQueueItemId(queueItemA.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(playbackA.getId());
    }

    private Room persistRoom(String code) {
        Room room = new Room(code, RoomStatus.ACTIVE);
        entityManager.persist(room);
        return room;
    }

    private Song persistSong(String videoId) {
        Song song = new Song(videoId, "Title", null, Duration.ofSeconds(100));
        entityManager.persist(song);
        return song;
    }

    private User persistUser(String googleId, String email) {
        User user = newUser(googleId, email);
        entityManager.persist(user);
        return user;
    }

    private QueueItem persistQueueItem(Room room, Song song, User user, int position) {
        QueueItem item = new QueueItem(room, song, user, Instant.now(), position);
        entityManager.persist(item);
        return item;
    }

    private Playback persistPlayback(QueueItem queueItem, Instant startedAt) {
        return playbackRepository.save(new Playback(queueItem, startedAt));
    }

    private Playback persistPlayback(QueueItem queueItem, Instant startedAt, PlaybackStatus status) {
        Playback playback = new Playback(queueItem, startedAt);
        ReflectionTestUtils.setField(playback, "status", status);
        return playbackRepository.save(playback);
    }

    private User newUser(String googleId, String email) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            ReflectionTestUtils.setField(user, "googleId", googleId);
            ReflectionTestUtils.setField(user, "name", "Test User");
            ReflectionTestUtils.setField(user, "email", email);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível criar User de teste", e);
        }
    }
}