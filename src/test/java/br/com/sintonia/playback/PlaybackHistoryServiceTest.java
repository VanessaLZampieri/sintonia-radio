package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackHistoryServiceTest {

    private static final Long ROOM_ID = 10L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PlaybackRepository playbackRepository;

    @InjectMocks
    private PlaybackHistoryService playbackHistoryService;

    private Room room;
    private User user;
    private Song song;

    @BeforeEach
    void setUp() {
        room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        user = newUser(100L);
        song = new Song("abc123", "Title", "https://img.jpg", Duration.ofSeconds(213));
    }

    @Test
    void historyReturnsOnlyFinishedStatuses() {
        Playback finished = playback(PlaybackStatus.FINISHED);
        Playback skipped = playback(PlaybackStatus.SKIPPED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(playbackRepository.findByQueueItemRoomIdAndStatusInOrderByStartedAtDesc(
                eq(ROOM_ID), any(List.class), any(Pageable.class)))
                .thenReturn(List.of(skipped, finished));

        List<PlaybackHistoryResponse> result = playbackHistoryService.history(ROOM_ID, 20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).playbackId()).isEqualTo(skipped.getId());
        assertThat(result.get(1).playbackId()).isEqualTo(finished.getId());
        assertThat(result.get(0).status()).isEqualTo(PlaybackStatus.SKIPPED);
        assertThat(result.get(0).song().youtubeVideoId()).isEqualTo("abc123");
        assertThat(result.get(0).addedBy().id()).isEqualTo(100L);
    }

    @Test
    void recentSongIdsMapsToYoutubeVideoId() {
        Playback playback = playback(PlaybackStatus.FINISHED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(playbackRepository.findByQueueItemRoomIdAndStatusInOrderByStartedAtDesc(
                eq(ROOM_ID), any(List.class), any(Pageable.class)))
                .thenReturn(List.of(playback));

        List<String> result = playbackHistoryService.recentSongIds(ROOM_ID, 10);

        assertThat(result).containsExactly("abc123");
    }

    @Test
    void historyRejectsUnknownRoom() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackHistoryService.history(ROOM_ID, 20))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void historyRejectsClosedRoom() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> playbackHistoryService.history(ROOM_ID, 20))
                .isInstanceOf(RoomClosedException.class);
    }

    private Playback playback(PlaybackStatus status) {
        QueueItem item = new QueueItem(room, song, user, Instant.parse("2026-01-01T00:00:00Z"), 1);
        ReflectionTestUtils.setField(item, "id", 10L);
        Playback playback = new Playback(item, Instant.parse("2026-01-01T19:30:00Z"));
        ReflectionTestUtils.setField(playback, "id", 100L);
        playback.setStatus(status);
        playback.setEndedAt(Instant.parse("2026-01-01T19:33:00Z"));
        return playback;
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
