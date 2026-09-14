package br.com.sintonia.room;

import br.com.sintonia.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomPlayerControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String SESSION_ID = "session-1";

    private MockMvc mockMvc;
    private RoomPlayerService roomPlayerService;

    @BeforeEach
    void setUp() {
        roomPlayerService = mock(RoomPlayerService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomPlayerController(roomPlayerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getsCurrentPlayer() throws Exception {
        RoomPlayerResponse response = new RoomPlayerResponse(ROOM_ID, SESSION_ID, USER_ID,
                Instant.parse("2026-09-09T18:30:00Z"));
        when(roomPlayerService.current(ROOM_ID)).thenReturn(response);

        mockMvc.perform(get("/api/rooms/{roomId}/player", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.clientSessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.userId").value(100))
                .andExpect(jsonPath("$.assumedAt").value("2026-09-09T18:30:00Z"));

        verify(roomPlayerService).current(ROOM_ID);
    }

    @Test
    void claimsPlayer() throws Exception {
        RoomPlayerResponse response = new RoomPlayerResponse(ROOM_ID, SESSION_ID, USER_ID,
                Instant.parse("2026-09-09T18:30:00Z"));
        when(roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID)).thenReturn(response);

        mockMvc.perform(post("/api/rooms/{roomId}/player", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"session-1\",\"userId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.userId").value(100));

        verify(roomPlayerService).claim(ROOM_ID, SESSION_ID, USER_ID);
    }

    @Test
    void releasesPlayer() throws Exception {
        RoomPlayerResponse response = new RoomPlayerResponse(ROOM_ID, null, null, null);
        when(roomPlayerService.release(ROOM_ID, SESSION_ID)).thenReturn(response);

        mockMvc.perform(delete("/api/rooms/{roomId}/player", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"session-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1));

        verify(roomPlayerService).release(ROOM_ID, SESSION_ID);
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExist() throws Exception {
        when(roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .thenThrow(new RoomNotFoundException("Sala não encontrada."));

        mockMvc.perform(post("/api/rooms/{roomId}/player", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"session-1\",\"userId\":100}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sala não encontrada."));
    }

    @Test
    void returnsConflictWhenPlayerAlreadyClaimed() throws Exception {
        when(roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .thenThrow(new PlayerAlreadyClaimedException("Já existe um player nesta sala."));

        mockMvc.perform(post("/api/rooms/{roomId}/player", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"session-1\",\"userId\":100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe um player nesta sala."));
    }

    @Test
    void returnsForbiddenWhenNotThePlayer() throws Exception {
        when(roomPlayerService.release(ROOM_ID, SESSION_ID))
                .thenThrow(new NotThePlayerException("A sessão informada não é o player atual."));

        mockMvc.perform(delete("/api/rooms/{roomId}/player", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"session-1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("A sessão informada não é o player atual."));
    }

    @Test
    void returnsBadRequestWhenSessionIdInvalid() throws Exception {
        when(roomPlayerService.claim(ROOM_ID, "   ", USER_ID))
                .thenThrow(new InvalidClientSessionIdException("clientSessionId é obrigatório."));

        mockMvc.perform(post("/api/rooms/{roomId}/player", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"   \",\"userId\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("clientSessionId é obrigatório."));
    }
}
