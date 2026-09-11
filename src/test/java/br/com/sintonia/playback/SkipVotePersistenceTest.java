package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
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
class SkipVotePersistenceTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndRecoversSkipVote() {
        Room room = new Room("STEST123", RoomStatus.ACTIVE);
        Song song = new Song("sv-song-1", "Title", null, Duration.ofSeconds(100));
        User user = newUser("sv-g1", "Skip Vote User", "sv@example.com");
        entityManager.persist(room);
        entityManager.persist(song);
        entityManager.persist(user);

        QueueItem queueItem = new QueueItem(room, song, user, Instant.parse("2026-01-01T00:00:00Z"), 1);
        entityManager.persist(queueItem);
        entityManager.flush();

        Playback playback = new Playback(queueItem, Instant.parse("2026-01-01T19:30:00Z"));
        entityManager.persist(playback);
        entityManager.flush();

        Instant createdAt = Instant.parse("2026-01-01T19:31:00Z");
        SkipVote vote = new SkipVote(playback, user, createdAt);
        entityManager.persist(vote);
        entityManager.flush();
        entityManager.clear();

        SkipVote found = entityManager.find(SkipVote.class, vote.getId());

        assertThat(found).isNotNull();
        assertThat(found.getPlayback().getId()).isEqualTo(playback.getId());
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void rejectsDuplicateVoteForSamePlaybackAndUser() {
        Room room = new Room("STEST124", RoomStatus.ACTIVE);
        Song song = new Song("sv-song-2", "Title", null, Duration.ofSeconds(100));
        User user = newUser("sv-g2", "Skip Vote User 2", "sv2@example.com");
        entityManager.persist(room);
        entityManager.persist(song);
        entityManager.persist(user);

        QueueItem queueItem = new QueueItem(room, song, user, Instant.parse("2026-01-01T00:00:00Z"), 1);
        entityManager.persist(queueItem);
        entityManager.flush();

        Playback playback = new Playback(queueItem, Instant.parse("2026-01-01T19:30:00Z"));
        entityManager.persist(playback);
        entityManager.flush();

        SkipVote first = new SkipVote(playback, user, Instant.parse("2026-01-01T19:31:00Z"));
        entityManager.persist(first);
        entityManager.flush();

        SkipVote second = new SkipVote(playback, user, Instant.parse("2026-01-01T19:32:00Z"));

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