package br.com.sintonia.queue;

import br.com.sintonia.exception.GlobalExceptionHandler;
import br.com.sintonia.playback.PlaybackService;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.security.SintoniaOAuth2User;
import br.com.sintonia.song.Song;
import br.com.sintonia.song.SongNotFoundException;
import br.com.sintonia.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QueueControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long SONG_ID = 123L;
    private static final Long USER_ID = 456L;

    private MockMvc mockMvc;
    private QueueService queueService;
    private PlaybackService playbackService;

    @BeforeEach
    void setUp() {
        queueService = mock(QueueService.class);
        playbackService = mock(PlaybackService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new QueueController(queueService, playbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addsSongToQueue() throws Exception {
        QueueItem item = buildQueueItem();
        when(queueService.add(ROOM_ID, SONG_ID, USER_ID)).thenReturn(item);

        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"songId\": 123}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.song.id").value(123))
                .andExpect(jsonPath("$.addedBy.id").value(456))
                .andExpect(jsonPath("$.addedAt").value("2026-09-09T18:30:00Z"));

        verify(queueService).add(ROOM_ID, SONG_ID, USER_ID);
    }

    @Test
    void addTriggersPlaybackWhenRoomIdle() throws Exception {
        QueueItem item = buildQueueItem();
        when(queueService.add(ROOM_ID, SONG_ID, USER_ID)).thenReturn(item);

        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"songId\": 123}"))
                .andExpect(status().isCreated());

        verify(queueService).add(ROOM_ID, SONG_ID, USER_ID);
        verify(playbackService).ensurePlayback(ROOM_ID);
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExist() throws Exception {
        when(queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .thenThrow(new RoomNotFoundException("Sala não encontrada."));

        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"songId\": 123}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sala não encontrada."));
    }

    @Test
    void returnsConflictWhenRoomIsClosed() throws Exception {
        when(queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .thenThrow(new RoomClosedException("Esta sala foi encerrada."));

        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"songId\": 123}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta sala foi encerrada."));
    }

    @Test
    void returnsNotFoundWhenSongDoesNotExist() throws Exception {
        when(queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .thenThrow(new SongNotFoundException("Música não encontrada."));

        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"songId\": 123}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Música não encontrada."));
    }

    @Test
    void returnsForbiddenWhenUserNotInRoom() throws Exception {
        when(queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .thenThrow(new UserNotInRoomException("Usuário não está na sala."));

        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"songId\": 123}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Usuário não está na sala."));
    }

    @Test
    void returnsConflictWhenSongAlreadyInQueue() throws Exception {
        when(queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .thenThrow(new SongAlreadyInQueueException("Esta música já está na fila."));

        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"songId\": 123}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta música já está na fila."));
    }

    @Test
    void returnsConflictWhenQueueLimitExceeded() throws Exception {
        when(queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .thenThrow(new QueueLimitExceededException(
                        "O usuário já possui 8 músicas na fila."));

        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"songId\": 123}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O usuário já possui 8 músicas na fila."));
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/rooms/{roomId}/queue", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(queueService, never()).add(ROOM_ID, SONG_ID, USER_ID);
    }

    @Test
    void returnsQueueWhenRoomIsActive() throws Exception {
        QueueItem item = buildQueueItem();
        when(queueService.findQueue(ROOM_ID)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/rooms/{roomId}/queue", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].song.id").value(123))
                .andExpect(jsonPath("$[0].addedBy.id").value(456))
                .andExpect(jsonPath("$[0].addedAt").value("2026-09-09T18:30:00Z"));

        verify(queueService).findQueue(ROOM_ID);
    }

    @Test
    void returnsEmptyListWhenRoomIsActiveWithNoItems() throws Exception {
        when(queueService.findQueue(ROOM_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/rooms/{roomId}/queue", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(queueService).findQueue(ROOM_ID);
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExistForQueue() throws Exception {
        when(queueService.findQueue(ROOM_ID))
                .thenThrow(new RoomNotFoundException("Sala não encontrada."));

        mockMvc.perform(get("/api/rooms/{roomId}/queue", ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sala não encontrada."));
    }

    @Test
    void returnsConflictWhenRoomIsClosedForQueue() throws Exception {
        when(queueService.findQueue(ROOM_ID))
                .thenThrow(new RoomClosedException("Esta sala foi encerrada."));

        mockMvc.perform(get("/api/rooms/{roomId}/queue", ROOM_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta sala foi encerrada."));
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExistForDelete() throws Exception {
        doThrow(new RoomNotFoundException("Sala não encontrada."))
                .when(queueService).remove(ROOM_ID, 10L, USER_ID);

        mockMvc.perform(delete("/api/rooms/{roomId}/queue/{queueItemId}", ROOM_ID, 10L)
                        .with(auth(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sala não encontrada."));
    }

    @Test
    void returnsConflictWhenRoomIsClosedForDelete() throws Exception {
        doThrow(new RoomClosedException("Esta sala foi encerrada."))
                .when(queueService).remove(ROOM_ID, 10L, USER_ID);

        mockMvc.perform(delete("/api/rooms/{roomId}/queue/{queueItemId}", ROOM_ID, 10L)
                        .with(auth(USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta sala foi encerrada."));
    }

    @Test
    void returnsForbiddenWhenUserNotInRoomForDelete() throws Exception {
        doThrow(new UserNotInRoomException("Usuário não está na sala."))
                .when(queueService).remove(ROOM_ID, 10L, USER_ID);

        mockMvc.perform(delete("/api/rooms/{roomId}/queue/{queueItemId}", ROOM_ID, 10L)
                        .with(auth(USER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Usuário não está na sala."));
    }

    @Test
    void returnsNotFoundWhenItemNotFoundForDelete() throws Exception {
        doThrow(new QueueItemNotFoundException("Item da fila não encontrado."))
                .when(queueService).remove(ROOM_ID, 10L, USER_ID);

        mockMvc.perform(delete("/api/rooms/{roomId}/queue/{queueItemId}", ROOM_ID, 10L)
                        .with(auth(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item da fila não encontrado."));
    }

    @Test
    void removesItemSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/rooms/{roomId}/queue/{queueItemId}", ROOM_ID, 10L)
                        .with(auth(USER_ID)))
                .andExpect(status().isOk());

        verify(queueService).remove(ROOM_ID, 10L, USER_ID);
    }

    private RequestPostProcessor auth(Long userId) {
        OidcUser delegate = mock(OidcUser.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new SintoniaOAuth2User(delegate, userId), null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return request -> {
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    private QueueItem buildQueueItem() {
        Room room = mock(Room.class);
        Song song = mock(Song.class);
        User user = mock(User.class);

        when(room.getId()).thenReturn(1L);
        when(song.getId()).thenReturn(123L);
        when(song.getYoutubeVideoId()).thenReturn("abc123");
        when(song.getTitle()).thenReturn("Title");
        when(song.getThumbnailUrl()).thenReturn("https://img/1.jpg");
        when(song.getDuration()).thenReturn(Duration.ofSeconds(213));
        when(user.getId()).thenReturn(456L);
        when(user.getName()).thenReturn("Test User");
        when(user.getAvatarUrl()).thenReturn(null);

        QueueItem item = new QueueItem(room, song, user, Instant.parse("2026-09-09T18:30:00Z"), 1);
        ReflectionTestUtils.setField(item, "id", 10L);

        return item;
    }
}
