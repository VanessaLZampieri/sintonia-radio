package br.com.sintonia.room;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomActivityControllerTest {

    private static final Long ROOM_ID = 1L;

    private MockMvc mockMvc;
    private RoomActivityService roomActivityService;

    @BeforeEach
    void setUp() {
        roomActivityService = mock(RoomActivityService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomActivityController(roomActivityService)).build();
    }

    @Test
    void listsActivities() throws Exception {
        when(roomActivityService.findRecent(ROOM_ID)).thenReturn(List.of(
                new RoomActivityResponse(10L, RoomActivityType.SONG_ADDED, 5L, "Ana", 1L, "Everlong", null,
                        Instant.parse("2026-09-23T12:00:00Z")),
                new RoomActivityResponse(9L, RoomActivityType.MEMBER_JOINED, 5L, "Ana", null, null, null,
                        Instant.parse("2026-09-23T11:59:00Z"))));

        mockMvc.perform(get("/api/rooms/{roomId}/activities", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("SONG_ADDED"))
                .andExpect(jsonPath("$[0].actorDisplayName").value("Ana"))
                .andExpect(jsonPath("$[0].songTitle").value("Everlong"))
                .andExpect(jsonPath("$[1].type").value("MEMBER_JOINED"));
    }
}
