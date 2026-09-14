package br.com.sintonia.room;

import br.com.sintonia.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomPlaybackModeControllerTest {

    private static final Long ROOM_ID = 1L;

    private MockMvc mockMvc;
    private RoomPlaybackModeService roomPlaybackModeService;

    @BeforeEach
    void setUp() {
        roomPlaybackModeService = mock(RoomPlaybackModeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomPlaybackModeController(roomPlaybackModeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getsCurrentMode() throws Exception {
        when(roomPlaybackModeService.current(ROOM_ID))
                .thenReturn(new PlaybackModeResponse(ROOM_ID, PlaybackMode.TODOS_OS_NAVEGADORES));

        mockMvc.perform(get("/api/rooms/{roomId}/playback-mode", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.mode").value("TODOS_OS_NAVEGADORES"));

        verify(roomPlaybackModeService).current(ROOM_ID);
    }

    @Test
    void changesMode() throws Exception {
        when(roomPlaybackModeService.change(ROOM_ID, PlaybackMode.CAIXA_DE_MUSICA))
                .thenReturn(new PlaybackModeResponse(ROOM_ID, PlaybackMode.CAIXA_DE_MUSICA));

        mockMvc.perform(put("/api/rooms/{roomId}/playback-mode", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"mode\":\"CAIXA_DE_MUSICA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("CAIXA_DE_MUSICA"));

        verify(roomPlaybackModeService).change(ROOM_ID, PlaybackMode.CAIXA_DE_MUSICA);
    }

    @Test
    void rejectsMissingMode() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/playback-mode", ROOM_ID)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExist() throws Exception {
        when(roomPlaybackModeService.change(ROOM_ID, PlaybackMode.CAIXA_DE_MUSICA))
                .thenThrow(new RoomNotFoundException("Sala não encontrada."));

        mockMvc.perform(put("/api/rooms/{roomId}/playback-mode", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"mode\":\"CAIXA_DE_MUSICA\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sala não encontrada."));
    }
}
