package br.com.sintonia.song;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SongTest {

    @Test
    void createsValidSong() {
        Song song = new Song("dQw4w9WgXcQ", "Never Gonna Give You Up",
                "https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg", Duration.ofSeconds(212));

        assertThat(song.getYoutubeVideoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(song.getTitle()).isEqualTo("Never Gonna Give You Up");
        assertThat(song.getThumbnailUrl()).isEqualTo("https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg");
        assertThat(song.getDuration()).isEqualTo(Duration.ofSeconds(212));
    }

    @Test
    void allowsNullThumbnail() {
        Song song = new Song("dQw4w9WgXcQ", "Never Gonna Give You Up", null, Duration.ofSeconds(212));

        assertThat(song.getThumbnailUrl()).isNull();
    }

    @Test
    void preservesPositiveDuration() {
        Duration duration = Duration.ofMinutes(3).plusSeconds(42);

        Song song = new Song("dQw4w9WgXcQ", "Title", null, duration);

        assertThat(song.getDuration()).isEqualTo(duration);
    }

    @Test
    void rejectsNegativeDuration() {
        assertThatThrownBy(() -> new Song("dQw4w9WgXcQ", "Title", null, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullYoutubeVideoId() {
        assertThatThrownBy(() -> new Song(null, "Title", null, Duration.ofSeconds(10)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullTitle() {
        assertThatThrownBy(() -> new Song("dQw4w9WgXcQ", null, null, Duration.ofSeconds(10)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullDuration() {
        assertThatThrownBy(() -> new Song("dQw4w9WgXcQ", "Title", null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
