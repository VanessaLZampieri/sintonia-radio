package br.com.sintonia.song;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SongPersistenceTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndRecoversSong() {
        Song song = new Song("dQw4w9WgXcQ", "Never Gonna Give You Up",
                "https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg", Duration.ofSeconds(212));

        entityManager.persist(song);
        entityManager.flush();
        entityManager.clear();

        Song found = entityManager.find(Song.class, song.getId());

        assertThat(found.getYoutubeVideoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(found.getTitle()).isEqualTo("Never Gonna Give You Up");
        assertThat(found.getThumbnailUrl()).isEqualTo("https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg");
        assertThat(found.getDuration()).isEqualTo(Duration.ofSeconds(212));
    }

    @Test
    void rejectsDuplicateYoutubeVideoId() {
        Song first = new Song("dQw4w9WgXcQ", "Title A", null, Duration.ofSeconds(10));
        entityManager.persist(first);
        entityManager.flush();

        Song second = new Song("dQw4w9WgXcQ", "Title B", null, Duration.ofSeconds(20));

        assertThatThrownBy(() -> {
            entityManager.persist(second);
            entityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }
}
