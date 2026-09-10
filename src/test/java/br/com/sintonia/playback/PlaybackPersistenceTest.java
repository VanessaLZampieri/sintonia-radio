package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PlaybackPersistenceTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndRecoversPlayback() {
        Room room = new Room("PTEST123", RoomStatus.ACTIVE);
        Song song = new Song("pb-song-1", "Title", null, Duration.ofSeconds(100));
        User user = newUser("pb-g1", "Playback User", "pb@example.com");
        entityManager.persist(room);
        entityManager.persist(song);
        entityManager.persist(user);

        QueueItem queueItem = new QueueItem(room, song, user, Instant.parse("2026-01-01T00:00:00Z"), 1);
        entityManager.persist(queueItem);
        entityManager.flush();

        Instant startedAt = Instant.parse("2026-01-01T19:30:00Z");
        Playback playback = new Playback(queueItem, startedAt);
        entityManager.persist(playback);
        entityManager.flush();
        entityManager.clear();

        Playback found = entityManager.find(Playback.class, playback.getId());

        assertThat(found).isNotNull();
        assertThat(found.getQueueItem().getId()).isEqualTo(queueItem.getId());
        assertThat(found.getStartedAt()).isEqualTo(startedAt);
        assertThat(found.getEndedAt()).isNull();
        assertThat(found.getStatus()).isEqualTo(PlaybackStatus.PLAYING);
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