package br.com.sintonia.playback;

import br.com.sintonia.exception.GlobalExceptionHandler;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.UserNotInRoomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SkipVoteControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;

    private MockMvc mockMvc;
    private SkipVoteService skipVoteService;

    @BeforeEach
    void setUp() {
        skipVoteService = mock(SkipVoteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SkipVoteController(skipVoteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void votes() throws Exception {
        when(skipVoteService.vote(ROOM_ID, USER_ID))
                .thenReturn(new SkipVoteResponse(1L, 2L, 3L, false));

        mockMvc.perform(post("/api/rooms/{roomId}/skip-votes", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"userId\": 100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playbackId").value(1))
                .andExpect(jsonPath("$.votes").value(2))
                .andExpect(jsonPath("$.requiredVotes").value(3))
                .andExpect(jsonPath("$.skipped").value(false));

        verify(skipVoteService).vote(ROOM_ID, USER_ID);
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/rooms/{roomId}/skip-votes", ROOM_ID)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsForbiddenWhenUserNotInRoom() throws Exception {
        when(skipVoteService.vote(ROOM_ID, USER_ID))
                .thenThrow(new UserNotInRoomException("Usuário não está na sala."));

        mockMvc.perform(post("/api/rooms/{roomId}/skip-votes", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"userId\": 100}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsConflictWhenAlreadyVoted() throws Exception {
        when(skipVoteService.vote(ROOM_ID, USER_ID))
                .thenThrow(new SkipVoteAlreadyExistsException("O usuário já votou neste playback."));

        mockMvc.perform(post("/api/rooms/{roomId}/skip-votes", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"userId\": 100}"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundWhenNoPlayingPlayback() throws Exception {
        when(skipVoteService.vote(ROOM_ID, USER_ID))
                .thenThrow(new PlaybackNotFoundException("Não há playback em andamento na sala."));

        mockMvc.perform(post("/api/rooms/{roomId}/skip-votes", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"userId\": 100}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExist() throws Exception {
        when(skipVoteService.vote(ROOM_ID, USER_ID))
                .thenThrow(new RoomNotFoundException("Sala não encontrada."));

        mockMvc.perform(post("/api/rooms/{roomId}/skip-votes", ROOM_ID)
                        .contentType("application/json")
                        .content("{\"userId\": 100}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getsCurrentVoteState() throws Exception {
        when(skipVoteService.current(ROOM_ID))
                .thenReturn(Optional.of(new SkipVoteResponse(1L, 2L, 3L, false)));

        mockMvc.perform(get("/api/rooms/{roomId}/skip-votes", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes").value(2))
                .andExpect(jsonPath("$.requiredVotes").value(3));

        verify(skipVoteService).current(ROOM_ID);
    }

    @Test
    void returnsNotFoundWhenNoCurrentVote() throws Exception {
        when(skipVoteService.current(ROOM_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rooms/{roomId}/skip-votes", ROOM_ID))
                .andExpect(status().isNotFound());
    }
}
