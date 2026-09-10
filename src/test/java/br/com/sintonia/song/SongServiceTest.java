package br.com.sintonia.song;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongService songService;

    @Test
    void reusesExistingSong() {
        Song existing = new Song("abc123", "Existing Title", null, Duration.ofSeconds(100));
        when(songRepository.findByYoutubeVideoId("abc123")).thenReturn(Optional.of(existing));

        Song result = songService.findOrCreate("abc123", "Existing Title", null, Duration.ofSeconds(100));

        assertThat(result).isSameAs(existing);
        verify(songRepository, never()).save(any());
    }

    @Test
    void createsNewSongWhenNotFound() {
        when(songRepository.findByYoutubeVideoId("abc123")).thenReturn(Optional.empty());
        when(songRepository.save(any(Song.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Song result = songService.findOrCreate("abc123", "New Title", "https://img/1.jpg", Duration.ofSeconds(100));

        assertThat(result.getYoutubeVideoId()).isEqualTo("abc123");
        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getThumbnailUrl()).isEqualTo("https://img/1.jpg");
        assertThat(result.getDuration()).isEqualTo(Duration.ofSeconds(100));
        verify(songRepository).save(any(Song.class));
    }
}
