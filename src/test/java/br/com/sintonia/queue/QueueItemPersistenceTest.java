package br.com.sintonia.queue;

import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class QueueItemPersistenceTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndRecoversQueueItem() {
        Room room = new Room("QTEST123", RoomStatus.ACTIVE);
        Song song = new Song("qitem-song-1", "Title", null, Duration.ofSeconds(100));
        User user = newUser("qitem-g1", "Queue Item User", "qitem@example.com");

        entityManager.persist(room);
        entityManager.persist(song);
        entityManager.persist(user);

        Instant addedAt = Instant.parse("2026-01-01T00:00:00Z");
        QueueItem item = new QueueItem(room, song, user, addedAt, 3);
        entityManager.persist(item);
        entityManager.flush();
        entityManager.clear();

        QueueItem found = entityManager.find(QueueItem.class, item.getId());

        assertThat(found).isNotNull();
        assertThat(found.getRoom().getId()).isEqualTo(room.getId());
        assertThat(found.getSong().getId()).isEqualTo(song.getId());
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getAddedAt()).isEqualTo(addedAt);
        assertThat(found.getPosition()).isEqualTo(3);
        assertThat(found.getStatus()).isEqualTo(QueueItemStatus.WAITING);
    }

    @Test
    void allowsSinglePlayingPerRoom() {
        Room room = new Room("QTEST200", RoomStatus.ACTIVE);
        Song song = new Song("qitem-song-200", "Title", null, Duration.ofSeconds(100));
        User user = newUser("qitem-g200", "User", "user200@example.com");
        entityManager.persist(room);
        entityManager.persist(song);
        entityManager.persist(user);

        QueueItem item = new QueueItem(room, song, user, Instant.now(), 1);
        item.setStatus(QueueItemStatus.PLAYING);
        entityManager.persist(item);
        entityManager.flush();

        assertThat(item.getId()).isNotNull();
    }

    @Test
    void rejectsSecondPlayingInSameRoom() {
        Room room = new Room("QTEST201", RoomStatus.ACTIVE);
        Song song1 = new Song("qitem-song-201a", "Title", null, Duration.ofSeconds(100));
        Song song2 = new Song("qitem-song-201b", "Title", null, Duration.ofSeconds(100));
        User user = newUser("qitem-g201", "User", "user201@example.com");
        entityManager.persist(room);
        entityManager.persist(song1);
        entityManager.persist(song2);
        entityManager.persist(user);

        QueueItem first = new QueueItem(room, song1, user, Instant.now(), 1);
        first.setStatus(QueueItemStatus.PLAYING);
        entityManager.persist(first);
        entityManager.flush();

        QueueItem second = new QueueItem(room, song2, user, Instant.now(), 2);
        second.setStatus(QueueItemStatus.PLAYING);

        assertThatThrownBy(() -> {
            entityManager.persist(second);
            entityManager.flush();
        })
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> {
                    ConstraintViolationException cve = (ConstraintViolationException) e;
                    assertThat(cve.getSQLState()).isEqualTo("23505");
                });
    }

    @Test
    void allowsPlayingInDifferentRooms() {
        Room roomA = new Room("QTEST202", RoomStatus.ACTIVE);
        Room roomB = new Room("QTEST203", RoomStatus.ACTIVE);
        Song songA = new Song("qitem-song-202a", "Title", null, Duration.ofSeconds(100));
        Song songB = new Song("qitem-song-202b", "Title", null, Duration.ofSeconds(100));
        User user = newUser("qitem-g202", "User", "user202@example.com");
        entityManager.persist(roomA);
        entityManager.persist(roomB);
        entityManager.persist(songA);
        entityManager.persist(songB);
        entityManager.persist(user);

        QueueItem itemA = new QueueItem(roomA, songA, user, Instant.now(), 1);
        itemA.setStatus(QueueItemStatus.PLAYING);
        QueueItem itemB = new QueueItem(roomB, songB, user, Instant.now(), 1);
        itemB.setStatus(QueueItemStatus.PLAYING);
        entityManager.persist(itemA);
        entityManager.persist(itemB);
        entityManager.flush();

        assertThat(itemA.getId()).isNotNull();
        assertThat(itemB.getId()).isNotNull();
    }

    @Test
    void allowsMultipleWaitingInSameRoom() {
        Room room = new Room("QTEST204", RoomStatus.ACTIVE);
        Song song1 = new Song("qitem-song-204a", "Title", null, Duration.ofSeconds(100));
        Song song2 = new Song("qitem-song-204b", "Title", null, Duration.ofSeconds(100));
        Song song3 = new Song("qitem-song-204c", "Title", null, Duration.ofSeconds(100));
        User user = newUser("qitem-g204", "User", "user204@example.com");
        entityManager.persist(room);
        entityManager.persist(song1);
        entityManager.persist(song2);
        entityManager.persist(song3);
        entityManager.persist(user);

        QueueItem item1 = new QueueItem(room, song1, user, Instant.now(), 1);
        QueueItem item2 = new QueueItem(room, song2, user, Instant.now(), 2);
        QueueItem item3 = new QueueItem(room, song3, user, Instant.now(), 3);
        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.persist(item3);
        entityManager.flush();

        assertThat(item1.getId()).isNotNull();
        assertThat(item2.getId()).isNotNull();
        assertThat(item3.getId()).isNotNull();
    }

    private User newUser(String googleId, String name, String email) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            ReflectionTestUtils.setField(user, "googleId", googleId);
            ReflectionTestUtils.setField(user, "name", name);
            ReflectionTestUtils.setField(user, "email", email);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível criar User de teste", e);
        }
    }
}
