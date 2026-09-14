package br.com.sintonia.room;

import br.com.sintonia.playback.Playback;
import br.com.sintonia.playback.PlaybackRepository;
import br.com.sintonia.playback.PlaybackStatus;
import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomStateServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PlaybackRepository playbackRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @InjectMocks
    private RoomStateService roomStateService;

    private Room room;
    private User user;
    private Song song;

    @BeforeEach
    void setUp() {
        room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        user = newUser(USER_ID);
        song = new Song("abc123", "Title", "https://img.jpg", Duration.ofSeconds(213));
    }

    @Test
    void snapshotWithoutPlayerPlaybackOrQueue() {
        stubRoom();
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING)).thenReturn(Optional.empty());
        when(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID)).thenReturn(List.of());

        RoomStateResponse result = roomStateService.get(ROOM_ID);

        assertThat(result.roomId()).isEqualTo(ROOM_ID);
        assertThat(result.roomCode()).isEqualTo("ABCDEFGH");
        assertThat(result.status()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(result.playbackMode()).isEqualTo(PlaybackMode.TODOS_OS_NAVEGADORES);
        assertThat(result.player()).isNull();
        assertThat(result.currentPlayback()).isNull();
        assertThat(result.queue()).isEmpty();
    }

    @Test
    void snapshotWithPlayer() {
        room.changePlaybackMode(PlaybackMode.CAIXA_DE_MUSICA);
        room.claim(SESSION_ID, user);
        stubRoom();
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING)).thenReturn(Optional.empty());
        when(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID)).thenReturn(List.of());

        RoomStateResponse result = roomStateService.get(ROOM_ID);

        assertThat(result.playbackMode()).isEqualTo(PlaybackMode.CAIXA_DE_MUSICA);
        assertThat(result.player()).isNotNull();
        assertThat(result.player().clientSessionId()).isEqualTo(SESSION_ID);
        assertThat(result.player().userId()).isEqualTo(USER_ID);
    }

    @Test
    void snapshotWithCurrentPlayback() {
        QueueItem item = buildQueueItem(1);
        Playback playback = new Playback(item, Instant.parse("2026-09-09T18:30:00Z"));
        ReflectionTestUtils.setField(playback, "id", 100L);

        stubRoom();
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING)).thenReturn(Optional.of(playback));
        when(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID)).thenReturn(List.of());

        RoomStateResponse result = roomStateService.get(ROOM_ID);

        assertThat(result.currentPlayback()).isNotNull();
        assertThat(result.currentPlayback().playbackId()).isEqualTo(100L);
        assertThat(result.currentPlayback().queueItemId()).isEqualTo(10L);
        assertThat(result.currentPlayback().song().youtubeVideoId()).isEqualTo("abc123");
        assertThat(result.currentPlayback().song().title()).isEqualTo("Title");
        assertThat(result.currentPlayback().addedByUserId()).isEqualTo(USER_ID);
    }

    @Test
    void snapshotQueueOrdered() {
        QueueItem first = buildQueueItem(1);
        QueueItem second = buildQueueItem(2);

        stubRoom();
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING)).thenReturn(Optional.empty());
        when(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID)).thenReturn(List.of(first, second));

        RoomStateResponse result = roomStateService.get(ROOM_ID);

        assertThat(result.queue()).hasSize(2);
        assertThat(result.queue().get(0).position()).isEqualTo(1);
        assertThat(result.queue().get(1).position()).isEqualTo(2);
        assertThat(result.queue().get(0).song().youtubeVideoId()).isEqualTo("abc123");
        assertThat(result.queue().get(0).song().duration()).isEqualTo("PT3M33S");
        assertThat(result.queue().get(0).addedByUserId()).isEqualTo(USER_ID);
    }

    @Test
    void snapshotRejectsUnknownRoom() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomStateService.get(ROOM_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void snapshotRejectsClosedRoom() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> roomStateService.get(ROOM_ID))
                .isInstanceOf(RoomClosedException.class);
    }

    private void stubRoom() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
    }

    private QueueItem buildQueueItem(int position) {
        QueueItem item = new QueueItem(room, song, user, Instant.parse("2026-09-09T18:30:00Z"), position);
        ReflectionTestUtils.setField(item, "id", 10L);
        return item;
    }

    private User newUser(Long id) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            ReflectionTestUtils.setField(user, "id", id);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível criar User de teste", e);
        }
    }
}
