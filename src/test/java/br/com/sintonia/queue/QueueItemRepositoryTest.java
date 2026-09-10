package br.com.sintonia.queue;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QueueItemRepositoryTest {

    @Autowired
    private QueueItemRepository queueItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void existsWhenSongIsInRoom() {
        Room room = persistRoom("QT000001");
        Song song = persistSong("song-1");
        User user = persistUser("g1", "e1@example.com");
        persistQueueItem(room, song, user, 1);

        assertThat(queueItemRepository.existsByRoomIdAndSongId(room.getId(), song.getId())).isTrue();
    }

    @Test
    void doesNotExistWhenSongIsNotInRoom() {
        Room room = persistRoom("QT000002");
        Song song = persistSong("song-2");

        assertThat(queueItemRepository.existsByRoomIdAndSongId(room.getId(), song.getId())).isFalse();
    }

    @Test
    void scopesExistenceByRoom() {
        Room room1 = persistRoom("QT000003");
        Room room2 = persistRoom("QT000004");
        Song song = persistSong("song-3");
        User user = persistUser("g3", "e3@example.com");
        persistQueueItem(room1, song, user, 1);

        assertThat(queueItemRepository.existsByRoomIdAndSongId(room1.getId(), song.getId())).isTrue();
        assertThat(queueItemRepository.existsByRoomIdAndSongId(room2.getId(), song.getId())).isFalse();
    }

    @Test
    void countsSongsAddedByUser() {
        Room room = persistRoom("QT000005");
        User user = persistUser("g4", "e4@example.com");
        persistQueueItem(room, persistSong("song-4a"), user, 1);
        persistQueueItem(room, persistSong("song-4b"), user, 2);
        persistQueueItem(room, persistSong("song-4c"), user, 3);

        assertThat(queueItemRepository.countByRoomIdAndUserId(room.getId(), user.getId())).isEqualTo(3L);
    }

    @Test
    void countsIndependentlyPerUser() {
        Room room = persistRoom("QT000006");
        User user1 = persistUser("g5a", "e5a@example.com");
        User user2 = persistUser("g5b", "e5b@example.com");
        persistQueueItem(room, persistSong("song-5a"), user1, 1);
        persistQueueItem(room, persistSong("song-5b"), user1, 2);
        persistQueueItem(room, persistSong("song-5c"), user2, 3);

        assertThat(queueItemRepository.countByRoomIdAndUserId(room.getId(), user1.getId())).isEqualTo(2L);
        assertThat(queueItemRepository.countByRoomIdAndUserId(room.getId(), user2.getId())).isEqualTo(1L);
    }

    @Test
    void returnsMaxPosition() {
        Room room = persistRoom("QT000007");
        User user = persistUser("g6", "e6@example.com");
        persistQueueItem(room, persistSong("song-6a"), user, 1);
        persistQueueItem(room, persistSong("song-6b"), user, 2);
        persistQueueItem(room, persistSong("song-6c"), user, 3);

        assertThat(queueItemRepository.findMaxPositionByRoomId(room.getId())).isEqualTo(3);
    }

    @Test
    void returnsZeroForEmptyRoom() {
        Room room = persistRoom("QT000008");

        assertThat(queueItemRepository.findMaxPositionByRoomId(room.getId())).isEqualTo(0);
    }

    @Test
    void findAllByRoomIdOrderByPositionAscIdAsc_returnsItems() {
        Room room = persistRoom("QT000009");
        Song song = persistSong("song-9a");
        User user = persistUser("g9", "e9@example.com");
        persistQueueItem(room, song, user, 1);

        assertThat(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(room.getId())).hasSize(1);
    }

    @Test
    void findAllByRoomIdOrderByPositionAscIdAsc_returnsEmptyWhenNoItems() {
        Room room = persistRoom("QT000010");

        assertThat(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(room.getId())).isEmpty();
    }

    @Test
    void findAllByRoomIdOrderByPositionAscIdAsc_respectsPositionOrder() {
        Room room = persistRoom("QT000011");
        Song song1 = persistSong("song-11a");
        Song song2 = persistSong("song-11b");
        Song song3 = persistSong("song-11c");
        User user = persistUser("g11", "e11@example.com");
        persistQueueItem(room, song3, user, 3);
        persistQueueItem(room, song1, user, 1);
        persistQueueItem(room, song2, user, 2);

        var items = queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(room.getId());

        assertThat(items).hasSize(3);
        assertThat(items.get(0).getSong().getYoutubeVideoId()).isEqualTo("song-11a");
        assertThat(items.get(1).getSong().getYoutubeVideoId()).isEqualTo("song-11b");
        assertThat(items.get(2).getSong().getYoutubeVideoId()).isEqualTo("song-11c");
    }

    @Test
    void findAllByRoomIdOrderByPositionAscIdAsc_usesIdAsTiebreaker() {
        Room room = persistRoom("QT000012");
        Song song = persistSong("song-12");
        User user1 = persistUser("g12a", "e12a@example.com");
        User user2 = persistUser("g12b", "e12b@example.com");
        persistQueueItem(room, song, user1, 1);
        persistQueueItem(room, song, user2, 1);

        var items = queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(room.getId());

        assertThat(items).hasSize(2);
    }

    @Test
    void findAllByRoomIdOrderByPositionAscIdAsc_doesNotReturnOtherRoom() {
        Room room1 = persistRoom("QT000013");
        Room room2 = persistRoom("QT000014");
        Song song = persistSong("song-13");
        User user = persistUser("g13", "e13@example.com");
        persistQueueItem(room1, song, user, 1);

        assertThat(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(room2.getId())).isEmpty();
    }

    @Test
    void findByIdAndRoomId_returnsItemWhenFound() {
        Room room = persistRoom("QT000015");
        Song song = persistSong("song-15");
        User user = persistUser("g15", "e15@example.com");
        QueueItem item = persistQueueItem(room, song, user, 1);

        assertThat(queueItemRepository.findByIdAndRoomId(item.getId(), room.getId())).isPresent();
    }

    @Test
    void findByIdAndRoomId_returnsEmptyWhenItemNotFound() {
        Room room = persistRoom("QT000016");
        persistRoom("QT000017");

        assertThat(queueItemRepository.findByIdAndRoomId(99L, room.getId())).isEmpty();
    }

    @Test
    void findByIdAndRoomId_returnsEmptyWhenItemBelongsToOtherRoom() {
        Room room1 = persistRoom("QT000018");
        Room room2 = persistRoom("QT000019");
        Song song = persistSong("song-18");
        User user = persistUser("g18", "e18@example.com");
        QueueItem item = persistQueueItem(room1, song, user, 1);

        assertThat(queueItemRepository.findByIdAndRoomId(item.getId(), room2.getId())).isEmpty();
    }

    @Test
    void findByRoomIdAndStatus_returnsPlayingItem() {
        Room room = persistRoom("QT000020");
        Song song = persistSong("song-20");
        User user = persistUser("g20", "e20@example.com");
        persistQueueItem(room, song, user, 1, QueueItemStatus.PLAYING);

        Optional<QueueItem> result =
                queueItemRepository.findByRoomIdAndStatus(room.getId(), QueueItemStatus.PLAYING);

        assertThat(result).isPresent();
        assertThat(result.get().getSong().getYoutubeVideoId()).isEqualTo("song-20");
    }

    @Test
    void findByRoomIdAndStatus_returnsEmptyWhenNoPlaying() {
        Room room = persistRoom("QT000021");
        Song song = persistSong("song-21");
        User user = persistUser("g21", "e21@example.com");
        persistQueueItem(room, song, user, 1);

        assertThat(queueItemRepository.findByRoomIdAndStatus(room.getId(), QueueItemStatus.PLAYING)).isEmpty();
    }

    @Test
    void findByRoomIdAndStatus_doesNotReturnPlayingFromOtherRoom() {
        Room room1 = persistRoom("QT000022");
        Room room2 = persistRoom("QT000023");
        Song song = persistSong("song-22");
        User user = persistUser("g22", "e22@example.com");
        persistQueueItem(room1, song, user, 1, QueueItemStatus.PLAYING);

        assertThat(queueItemRepository.findByRoomIdAndStatus(room2.getId(), QueueItemStatus.PLAYING)).isEmpty();
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
        return queueItemRepository.save(new QueueItem(room, song, user, Instant.now(), position));
    }

    private QueueItem persistQueueItem(Room room, Song song, User user, int position, QueueItemStatus status) {
        QueueItem item = new QueueItem(room, song, user, Instant.now(), position);
        item.setStatus(status);
        return queueItemRepository.save(item);
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
