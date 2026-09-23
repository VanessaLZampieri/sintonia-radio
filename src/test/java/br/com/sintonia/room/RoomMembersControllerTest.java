package br.com.sintonia.room;

import br.com.sintonia.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomMembersControllerTest {

    private static final Long ROOM_ID = 1L;

    private MockMvc mockMvc;
    private RoomMemberService roomMemberService;

    @BeforeEach
    void setUp() {
        roomMemberService = mock(RoomMemberService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomMembersController(roomMemberService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPresentMembers() throws Exception {
        when(roomMemberService.findPresent(ROOM_ID)).thenReturn(List.of(
                new RoomParticipantResponse(10L, "Ana", "https://img/ana.jpg", 3L),
                new RoomParticipantResponse(11L, "Bruno", null, 0L)));

        mockMvc.perform(get("/api/rooms/{roomId}/members", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(10))
                .andExpect(jsonPath("$[0].displayName").value("Ana"))
                .andExpect(jsonPath("$[0].avatarUrl").value("https://img/ana.jpg"))
                .andExpect(jsonPath("$[0].waitingCount").value(3))
                .andExpect(jsonPath("$[1].userId").value(11))
                .andExpect(jsonPath("$[1].displayName").value("Bruno"))
                .andExpect(jsonPath("$[1].avatarUrl").isEmpty());
    }

    @Test
    void mapsClosedRoomToConflict() throws Exception {
        when(roomMemberService.findPresent(ROOM_ID))
                .thenThrow(new RoomClosedException("Esta sala foi encerrada."));

        mockMvc.perform(get("/api/rooms/{roomId}/members", ROOM_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta sala foi encerrada."));
    }
}
