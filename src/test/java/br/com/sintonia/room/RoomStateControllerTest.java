package br.com.sintonia.room;

import br.com.sintonia.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomStateControllerTest {

    private static final Long ROOM_ID = 1L;

    private MockMvc mockMvc;
    private RoomStateService roomStateService;

    @BeforeEach
    void setUp() {
        roomStateService = mock(RoomStateService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomStateController(roomStateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getsRoomState() throws Exception {
        RoomStateResponse response = new RoomStateResponse(
                ROOM_ID, "ABCDEFGH", RoomStatus.ACTIVE, PlaybackMode.TODOS_OS_NAVEGADORES,
                null, null, List.of());
        when(roomStateService.get(ROOM_ID)).thenReturn(response);

        mockMvc.perform(get("/api/rooms/{roomId}/state", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.roomCode").value("ABCDEFGH"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.playbackMode").value("TODOS_OS_NAVEGADORES"))
                .andExpect(jsonPath("$.queue").isEmpty());

        verify(roomStateService).get(ROOM_ID);
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExist() throws Exception {
        when(roomStateService.get(ROOM_ID)).thenThrow(new RoomNotFoundException("Sala não encontrada."));

        mockMvc.perform(get("/api/rooms/{roomId}/state", ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sala não encontrada."));
    }
}
