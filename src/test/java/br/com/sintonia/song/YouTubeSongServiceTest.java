package br.com.sintonia.song;

import br.com.sintonia.youtube.YouTubeClient;
import br.com.sintonia.youtube.YouTubeSearchResponse;
import br.com.sintonia.youtube.YouTubeVideoDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YouTubeSongServiceTest {

    @Mock
    private YouTubeClient youTubeClient;

    @Mock
    private SongService songService;

    @InjectMocks
    private YouTubeSongService youTubeSongService;

    @Test
    void searchDelegatesToYouTubeClientWithoutSaving() {
        YouTubeSearchResponse response = new YouTubeSearchResponse(List.of());
        when(youTubeClient.search("Nando Reis", 10)).thenReturn(response);

        YouTubeSearchResponse result = youTubeSongService.search("Nando Reis", 10);

        assertThat(result).isSameAs(response);
        verify(youTubeClient).search("Nando Reis", 10);
        verifyNoInteractions(songService);
    }

    @Test
    void selectReusesExistingSongWithoutCallingYouTube() {
        Song existing = new Song("abc123", "Existing", null, Duration.ofSeconds(100));
        when(songService.findByYoutubeVideoId("abc123")).thenReturn(Optional.of(existing));

        Optional<Song> result = youTubeSongService.select("abc123");

        assertThat(result).contains(existing);
        verify(youTubeClient, never()).getVideoDetails(any());
        verify(songService, never()).findOrCreate(any(), any(), any(), any());
    }

    @Test
    void selectFetchesDetailsAndCreatesNewSong() {
        YouTubeVideoDetails details = new YouTubeVideoDetails(
                "abc123", "New Title", "https://img/1.jpg", Duration.ofSeconds(100));
        Song created = new Song("abc123", "New Title", "https://img/1.jpg", Duration.ofSeconds(100));

        when(songService.findByYoutubeVideoId("abc123")).thenReturn(Optional.empty());
        when(youTubeClient.getVideoDetails("abc123")).thenReturn(Optional.of(details));
        when(songService.findOrCreate("abc123", "New Title", "https://img/1.jpg", Duration.ofSeconds(100)))
                .thenReturn(created);

        Optional<Song> result = youTubeSongService.select("abc123");

        assertThat(result).contains(created);
        verify(youTubeClient).getVideoDetails("abc123");
        verify(songService).findOrCreate("abc123", "New Title", "https://img/1.jpg", Duration.ofSeconds(100));
    }

    @Test
    void selectReturnsEmptyWhenVideoNotFound() {
        when(songService.findByYoutubeVideoId("missing")).thenReturn(Optional.empty());
        when(youTubeClient.getVideoDetails("missing")).thenReturn(Optional.empty());

        Optional<Song> result = youTubeSongService.select("missing");

        assertThat(result).isEmpty();
        verify(songService, never()).findOrCreate(any(), any(), any(), any());
    }
}
