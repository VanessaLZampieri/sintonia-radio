package br.com.sintonia.song;

import br.com.sintonia.youtube.YouTubeClient;
import br.com.sintonia.youtube.YouTubeSearchItem;
import br.com.sintonia.youtube.YouTubeSearchResponse;
import br.com.sintonia.youtube.YouTubeVideoDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void searchFiltersNonEmbeddableAndLive() {
        when(youTubeClient.search("q", 10)).thenReturn(response(item("a", "A"), item("b", "B"), item("c", "C")));
        when(youTubeClient.getVideoDetailsBatch(List.of("a", "b", "c"))).thenReturn(Map.of(
                "a", details("a", true, false),
                "b", details("b", false, false),
                "c", details("c", true, true)));

        List<SongSearchItemResponse> result = youTubeSongService.search("q", 10);

        assertThat(result).extracting(SongSearchItemResponse::videoId).containsExactly("a");
    }

    @Test
    void searchPreservesRelativeOrder() {
        when(youTubeClient.search("q", 10)).thenReturn(response(item("a", "A"), item("b", "B"), item("c", "C")));
        when(youTubeClient.getVideoDetailsBatch(List.of("a", "b", "c"))).thenReturn(Map.of(
                "a", details("a", true, false),
                "b", details("b", true, false),
                "c", details("c", true, false)));

        List<SongSearchItemResponse> result = youTubeSongService.search("q", 10);

        assertThat(result).extracting(SongSearchItemResponse::videoId).containsExactly("a", "b", "c");
    }

    @Test
    void searchDeduplicatesRepeatedVideoId() {
        when(youTubeClient.search("q", 10)).thenReturn(response(item("a", "A"), item("a", "A")));
        when(youTubeClient.getVideoDetailsBatch(List.of("a"))).thenReturn(Map.of("a", details("a", true, false)));

        List<SongSearchItemResponse> result = youTubeSongService.search("q", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).videoId()).isEqualTo("a");
    }

    @Test
    void searchEnrichesInBatch() {
        when(youTubeClient.search("q", 10)).thenReturn(response(item("a", "A"), item("b", "B")));
        when(youTubeClient.getVideoDetailsBatch(List.of("a", "b"))).thenReturn(Map.of(
                "a", details("a", true, false),
                "b", details("b", true, false)));

        youTubeSongService.search("q", 10);

        verify(youTubeClient).getVideoDetailsBatch(List.of("a", "b"));
        verify(youTubeClient, never()).getVideoDetails(any());
    }

    @Test
    void searchEnrichesWithChannelAndDuration() {
        when(youTubeClient.search("q", 10)).thenReturn(response(item("a", "Title A")));
        YouTubeVideoDetails d = new YouTubeVideoDetails("a", "Title A", "Channel X", "https://img/a.jpg",
                Duration.ofMinutes(4).plusSeconds(13), true, false);
        when(youTubeClient.getVideoDetailsBatch(List.of("a"))).thenReturn(Map.of("a", d));

        List<SongSearchItemResponse> result = youTubeSongService.search("q", 10);

        assertThat(result.get(0).channelTitle()).isEqualTo("Channel X");
        assertThat(result.get(0).thumbnailUrl()).isEqualTo("https://img/a.jpg");
        assertThat(result.get(0).duration()).isEqualTo("PT4M13S");
    }

    @Test
    void searchFiltersVideosLongerThanTwentyMinutes() {
        when(youTubeClient.search("q", 10)).thenReturn(response(item("short", "Short"), item("long", "Long")));
        when(youTubeClient.getVideoDetailsBatch(List.of("short", "long"))).thenReturn(Map.of(
                "short", new YouTubeVideoDetails("short", "Short", "Channel", null,
                        Duration.ofMinutes(20), true, false),
                "long", new YouTubeVideoDetails("long", "Long", "Channel", null,
                        Duration.ofMinutes(20).plusSeconds(1), true, false)));

        List<SongSearchItemResponse> result = youTubeSongService.search("q", 10);

        assertThat(result).extracting(SongSearchItemResponse::videoId).containsExactly("short");
    }

    @Test
    void selectReusesExistingSongWithoutCallingYouTube() {
        Song existing = new Song("abc123", "Existing", null, Duration.ofSeconds(100));
        when(songService.findByYoutubeVideoId("abc123")).thenReturn(Optional.of(existing));

        Optional<Song> result = youTubeSongService.select("abc123");

        assertThat(result).contains(existing);
        verify(youTubeClient, never()).getVideoDetails(any());
        verify(songService, never()).findOrCreate(any(), any(), any(), any(), any());
    }

    @Test
    void selectFetchesDetailsAndCreatesNewSong() {
        YouTubeVideoDetails details = new YouTubeVideoDetails(
                "abc123", "New Title", "Channel", "https://img/1.jpg", Duration.ofSeconds(100), true, false);
        Song created = new Song("abc123", "New Title", "https://img/1.jpg", Duration.ofSeconds(100), "Channel");

        when(songService.findByYoutubeVideoId("abc123")).thenReturn(Optional.empty());
        when(youTubeClient.getVideoDetails("abc123")).thenReturn(Optional.of(details));
        when(songService.findOrCreate("abc123", "New Title", "https://img/1.jpg", Duration.ofSeconds(100), "Channel"))
                .thenReturn(created);

        Optional<Song> result = youTubeSongService.select("abc123");

        assertThat(result).contains(created);
        verify(youTubeClient).getVideoDetails("abc123");
        verify(songService).findOrCreate("abc123", "New Title", "https://img/1.jpg", Duration.ofSeconds(100), "Channel");
    }

    @Test
    void selectReturnsEmptyWhenVideoNotFound() {
        when(songService.findByYoutubeVideoId("missing")).thenReturn(Optional.empty());
        when(youTubeClient.getVideoDetails("missing")).thenReturn(Optional.empty());

        Optional<Song> result = youTubeSongService.select("missing");

        assertThat(result).isEmpty();
        verify(songService, never()).findOrCreate(any(), any(), any(), any(), any());
    }

    @Test
    void selectRejectsExistingSongLongerThanTwentyMinutes() {
        Song existing = new Song("long", "Long", null, Duration.ofMinutes(21));
        when(songService.findByYoutubeVideoId("long")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> youTubeSongService.select("long"))
                .isInstanceOf(SongDurationLimitExceededException.class);

        verify(youTubeClient, never()).getVideoDetails(any());
    }

    private YouTubeSearchResponse response(YouTubeSearchItem... items) {
        return new YouTubeSearchResponse(List.of(items));
    }

    private YouTubeSearchItem item(String videoId, String title) {
        return new YouTubeSearchItem(new YouTubeSearchItem.ItemId(videoId), new YouTubeSearchItem.Snippet(title));
    }

    private YouTubeVideoDetails details(String id, boolean embeddable, boolean live) {
        return new YouTubeVideoDetails(id, "Title " + id, "Channel", null, Duration.ofSeconds(100), embeddable, live);
    }
}
