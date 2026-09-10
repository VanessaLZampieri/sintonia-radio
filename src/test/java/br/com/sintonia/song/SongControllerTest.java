package br.com.sintonia.song;

import br.com.sintonia.youtube.YouTubeSearchItem;
import br.com.sintonia.youtube.YouTubeSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SongControllerTest {

    private MockMvc mockMvc;
    private YouTubeSongService youTubeSongService;

    @BeforeEach
    void setUp() {
        youTubeSongService = mock(YouTubeSongService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SongController(youTubeSongService)).build();
    }

    @Test
    void searchesSongs() throws Exception {
        YouTubeSearchResponse response = new YouTubeSearchResponse(List.of(
                new YouTubeSearchItem(new YouTubeSearchItem.ItemId("abc123"),
                        new YouTubeSearchItem.Snippet("Title A"))));
        when(youTubeSongService.search("Nando Reis", 10)).thenReturn(response);

        mockMvc.perform(get("/api/songs/search").param("q", "Nando Reis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].videoId").value("abc123"))
                .andExpect(jsonPath("$[0].title").value("Title A"));

        verify(youTubeSongService).search("Nando Reis", 10);
    }

    @Test
    void searchesSongsWithMaxResults() throws Exception {
        when(youTubeSongService.search("Nando Reis", 5)).thenReturn(new YouTubeSearchResponse(List.of()));

        mockMvc.perform(get("/api/songs/search").param("q", "Nando Reis").param("maxResults", "5"))
                .andExpect(status().isOk());

        verify(youTubeSongService).search("Nando Reis", 5);
    }

    @Test
    void rejectsSearchWithoutQuery() throws Exception {
        mockMvc.perform(get("/api/songs/search"))
                .andExpect(status().isBadRequest());

        verify(youTubeSongService, never()).search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void rejectsBlankQuery() throws Exception {
        mockMvc.perform(get("/api/songs/search").param("q", "   "))
                .andExpect(status().isBadRequest());

        verify(youTubeSongService, never()).search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void rejectsInvalidMaxResults() throws Exception {
        mockMvc.perform(get("/api/songs/search").param("q", "Nando Reis").param("maxResults", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/songs/search").param("q", "Nando Reis").param("maxResults", "1000"))
                .andExpect(status().isBadRequest());

        verify(youTubeSongService, never()).search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void selectsExistingSong() throws Exception {
        Song song = new Song("dQw4w9WgXcQ", "Never Gonna Give You Up", "https://img/1.jpg", Duration.ofSeconds(213));
        ReflectionTestUtils.setField(song, "id", 1L);
        when(youTubeSongService.select("dQw4w9WgXcQ")).thenReturn(Optional.of(song));

        mockMvc.perform(get("/api/songs/dQw4w9WgXcQ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.youtubeVideoId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.title").value("Never Gonna Give You Up"))
                .andExpect(jsonPath("$.thumbnailUrl").value("https://img/1.jpg"))
                .andExpect(jsonPath("$.duration").value("PT3M33S"));

        verify(youTubeSongService).select("dQw4w9WgXcQ");
    }

    @Test
    void returnsNotFoundWhenVideoDoesNotExist() throws Exception {
        when(youTubeSongService.select("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/songs/missing"))
                .andExpect(status().isNotFound());
    }
}
