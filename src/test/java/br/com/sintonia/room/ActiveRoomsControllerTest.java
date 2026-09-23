package br.com.sintonia.room;

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

class ActiveRoomsControllerTest {

    private MockMvc mockMvc;
    private RoomDiscoveryService roomDiscoveryService;

    @BeforeEach
    void setUp() {
        roomDiscoveryService = mock(RoomDiscoveryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ActiveRoomsController(roomDiscoveryService)).build();
    }

    @Test
    void listsActiveRooms() throws Exception {
        when(roomDiscoveryService.listActiveRooms()).thenReturn(List.of(
                new ActiveRoomResponse(
                        1L, "Sala A", "CODE1", 3L, 2L,
                        new ActiveRoomResponse.NowPlaying(
                                "Título", "vid", "https://img.jpg",
                                new ActiveRoomResponse.AddedBy(10L, "Ana", "https://img/ana.jpg")))));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(1))
                .andExpect(jsonPath("$[0].name").value("Sala A"))
                .andExpect(jsonPath("$[0].code").value("CODE1"))
                .andExpect(jsonPath("$[0].participantCount").value(3))
                .andExpect(jsonPath("$[0].waitingCount").value(2))
                .andExpect(jsonPath("$[0].nowPlaying.title").value("Título"))
                .andExpect(jsonPath("$[0].nowPlaying.youtubeVideoId").value("vid"))
                .andExpect(jsonPath("$[0].nowPlaying.addedBy.displayName").value("Ana"));
    }
}
