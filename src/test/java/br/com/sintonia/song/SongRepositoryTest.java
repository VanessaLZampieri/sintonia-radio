package br.com.sintonia.song;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SongRepositoryTest {

    @Autowired
    private SongRepository songRepository;

    @Test
    void findsSongByYoutubeVideoId() {
        Song song = new Song("ABC123", "Title A", "https://img/1.jpg", Duration.ofSeconds(100));
        songRepository.save(song);

        Optional<Song> found = songRepository.findByYoutubeVideoId("ABC123");

        assertThat(found).isPresent();
        assertThat(found.get().getYoutubeVideoId()).isEqualTo("ABC123");
        assertThat(found.get().getTitle()).isEqualTo("Title A");
        assertThat(found.get().getThumbnailUrl()).isEqualTo("https://img/1.jpg");
        assertThat(found.get().getDuration()).isEqualTo(Duration.ofSeconds(100));
    }

    @Test
    void returnsEmptyWhenSongDoesNotExist() {
        Optional<Song> found = songRepository.findByYoutubeVideoId("NAO_EXISTE");

        assertThat(found).isEmpty();
    }

    @Test
    void distinguishesDifferentSongs() {
        Song first = new Song("ABC123", "Title A", null, Duration.ofSeconds(100));
        Song second = new Song("XYZ789", "Title B", null, Duration.ofSeconds(200));
        songRepository.save(first);
        songRepository.save(second);

        Optional<Song> foundFirst = songRepository.findByYoutubeVideoId("ABC123");
        Optional<Song> foundSecond = songRepository.findByYoutubeVideoId("XYZ789");

        assertThat(foundFirst).isPresent();
        assertThat(foundFirst.get().getTitle()).isEqualTo("Title A");
        assertThat(foundSecond).isPresent();
        assertThat(foundSecond.get().getTitle()).isEqualTo("Title B");
    }
}
