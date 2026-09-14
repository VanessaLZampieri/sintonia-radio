package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemSource;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.song.SongRepository;
import br.com.sintonia.user.User;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoDjServiceTest {

    private static final Long ROOM_ID = 10L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @Mock
    private PlaybackRepository playbackRepository;

    @Mock
    private PlaybackHistoryService playbackHistoryService;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    private Random random;
    private AutoDjService autoDjService;

    private Room room;
    private Song song1;
    private Song song2;
    private User user;

    @BeforeEach
    void setUp() {
        room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        song1 = new Song("song-1", "Song 1", null, Duration.ofSeconds(100));
        song2 = new Song("song-2", "Song 2", null, Duration.ofSeconds(100));
        user = mock(User.class);
        random = mock(Random.class);
        autoDjService = new AutoDjService(roomRepository, songRepository, queueItemRepository,
                playbackRepository, playbackHistoryService, roomEventPublisher, random);
    }

    @Test
    void doesNotCreateWhenHumanQueueExists() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING))
                .thenReturn(Optional.of(mock(QueueItem.class)));

        assertThat(autoDjService.createNext(ROOM_ID)).isEmpty();
        verify(songRepository, never()).findAll();
    }

    @Test
    void createsAutoDjItemFromEligibleSong() {
        stubFullEligibility();
        stubChoiceOf(song1);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSource()).isEqualTo(QueueItemSource.AUTO_DJ);
        assertThat(result.getUser()).isNull();
        assertThat(result.getSong()).isSameAs(song1);
        assertThat(result.getStatus()).isEqualTo(QueueItemStatus.WAITING);
    }

    @Test
    void doesNotChooseCurrentlyPlayingSong() {
        stubBase();
        QueueItem playingItem = new QueueItem(room, song1, user, Instant.now(), 1);
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.of(new Playback(playingItem, Instant.now())));
        stubNoWaiting();
        stubNoRecent();
        stubChoiceOf(song2);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong()).isSameAs(song2);
    }

    @Test
    void doesNotChooseRecentSong() {
        stubBase();
        stubNoPlaying();
        stubNoWaiting();
        when(playbackHistoryService.recentSongIds(ROOM_ID, 10)).thenReturn(List.of("song-1"));
        stubChoiceOf(song2);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong()).isSameAs(song2);
    }

    @Test
    void doesNotChooseWaitingSong() {
        stubBase();
        stubNoPlaying();
        when(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID))
                .thenReturn(List.of(new QueueItem(room, song1, user, Instant.now(), 1)));
        stubNoRecent();
        stubChoiceOf(song2);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong()).isSameAs(song2);
    }

    @Test
    void doesNothingWhenNoEligibleSong() {
        stubBase();
        stubNoPlaying();
        stubNoWaiting();
        when(playbackHistoryService.recentSongIds(ROOM_ID, 10)).thenReturn(List.of("song-1"));
        when(songRepository.findAll()).thenReturn(List.of(song1));

        assertThat(autoDjService.createNext(ROOM_ID)).isEmpty();
        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void publishesQueueChangedEvent() {
        stubFullEligibility();
        stubChoiceOf(song1);

        autoDjService.createNext(ROOM_ID);

        verify(roomEventPublisher).publish(eq("ABCDEFGH"), any(RoomEvent.class));
    }

    private void stubChoiceOf(Song chosen) {
        when(random.nextInt(anyInt())).thenReturn(0);
        when(songRepository.findAll()).thenReturn(List.of(chosen));
        when(queueItemRepository.findMaxPositionByRoomId(ROOM_ID)).thenReturn(0);
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(inv -> {
            QueueItem saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            return saved;
        });
    }

    private void stubBase() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING))
                .thenReturn(Optional.empty());
    }

    private void stubNoPlaying() {
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());
    }

    private void stubNoWaiting() {
        when(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID)).thenReturn(List.of());
    }

    private void stubNoRecent() {
        when(playbackHistoryService.recentSongIds(ROOM_ID, 10)).thenReturn(List.of());
    }

    private void stubFullEligibility() {
        stubBase();
        stubNoPlaying();
        stubNoWaiting();
        stubNoRecent();
    }
}
