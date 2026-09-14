package br.com.sintonia.playback;

import br.com.sintonia.exception.GlobalExceptionHandler;
import br.com.sintonia.queue.AddedByResponse;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.song.SongResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaybackHistoryControllerTest {

    private static final Long ROOM_ID = 1L;

    private MockMvc mockMvc;
    private PlaybackHistoryService playbackHistoryService;

    @BeforeEach
    void setUp() {
        playbackHistoryService = mock(PlaybackHistoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlaybackHistoryController(playbackHistoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getsHistory() throws Exception {
        PlaybackHistoryResponse response = new PlaybackHistoryResponse(
                1L, 10L, new SongResponse(1L, "abc123", "Title", null, "PT3M33S"),
                Instant.parse("2026-01-01T19:30:00Z"), Instant.parse("2026-01-01T19:33:00Z"),
                PlaybackStatus.FINISHED, new AddedByResponse(100L, "Test User", null));
        when(playbackHistoryService.history(ROOM_ID, 20)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/rooms/{roomId}/history", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playbackId").value(1))
                .andExpect(jsonPath("$[0].status").value("FINISHED"))
                .andExpect(jsonPath("$[0].song.youtubeVideoId").value("abc123"));

        verify(playbackHistoryService).history(ROOM_ID, 20);
    }

    @Test
    void rejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/history", ROOM_ID).param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExist() throws Exception {
        when(playbackHistoryService.history(ROOM_ID, 20))
                .thenThrow(new RoomNotFoundException("Sala não encontrada."));

        mockMvc.perform(get("/api/rooms/{roomId}/history", ROOM_ID))
                .andExpect(status().isNotFound());
    }
}
