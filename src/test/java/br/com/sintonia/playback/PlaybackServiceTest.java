package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemNotFoundException;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
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
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long QUEUE_ITEM_ID = 42L;
    private static final Long PLAYBACK_ID = 101L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @Mock
    private PlaybackRepository playbackRepository;

    @InjectMocks
    private PlaybackService playbackService;

    private Room room;
    private Song song;
    private User user;

    @BeforeEach
    void setUp() {
        room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        song = new Song("abc123", "Title", null, Duration.ofSeconds(100));
        user = mock(User.class);
    }

    @Test
    void startsPlaybackForWaitingQueueItem() {
        QueueItem queueItem = waitingQueueItem();
        stubHappyPath(queueItem);

        Playback result = playbackService.start(ROOM_ID, QUEUE_ITEM_ID);

        assertThat(result).isNotNull();
    }

    @Test
    void createdPlaybackHasPlayingStatus() {
        QueueItem queueItem = waitingQueueItem();
        stubHappyPath(queueItem);

        Playback result = playbackService.start(ROOM_ID, QUEUE_ITEM_ID);

        assertThat(result.getStatus()).isEqualTo(PlaybackStatus.PLAYING);
    }

    @Test
    void createdPlaybackHasNullEndedAt() {
        QueueItem queueItem = waitingQueueItem();
        stubHappyPath(queueItem);

        Playback result = playbackService.start(ROOM_ID, QUEUE_ITEM_ID);

        assertThat(result.getEndedAt()).isNull();
    }

    @Test
    void queueItemChangesToPlaying() {
        QueueItem queueItem = waitingQueueItem();
        stubHappyPath(queueItem);

        playbackService.start(ROOM_ID, QUEUE_ITEM_ID);

        assertThat(queueItem.getStatus()).isEqualTo(QueueItemStatus.PLAYING);
    }

    @Test
    void playbackIsAssociatedWithCorrectQueueItem() {
        QueueItem queueItem = waitingQueueItem();
        stubHappyPath(queueItem);

        Playback result = playbackService.start(ROOM_ID, QUEUE_ITEM_ID);

        assertThat(result.getQueueItem()).isSameAs(queueItem);
    }

    @Test
    void doesNotStartWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackService.start(ROOM_ID, QUEUE_ITEM_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void doesNotStartWhenRoomIsClosed() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> playbackService.start(ROOM_ID, QUEUE_ITEM_ID))
                .isInstanceOf(RoomClosedException.class);

        verify(queueItemRepository, never()).findByIdAndRoomId(any(), any());
    }

    @Test
    void doesNotStartWhenQueueItemNotInRoom() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByIdAndRoomId(QUEUE_ITEM_ID, ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackService.start(ROOM_ID, QUEUE_ITEM_ID))
                .isInstanceOf(QueueItemNotFoundException.class);

        verify(playbackRepository, never()).save(any(Playback.class));
    }

    @Test
    void doesNotStartWhenQueueItemNotWaiting() {
        QueueItem playingItem = new QueueItem(room, song, user, Instant.now(), 1);
        playingItem.setStatus(QueueItemStatus.PLAYING);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByIdAndRoomId(QUEUE_ITEM_ID, ROOM_ID)).thenReturn(Optional.of(playingItem));

        assertThatThrownBy(() -> playbackService.start(ROOM_ID, QUEUE_ITEM_ID))
                .isInstanceOf(QueueItemNotWaitingException.class);

        verify(playbackRepository, never()).save(any(Playback.class));
    }

    @Test
    void doesNotStartWhenAlreadyPlaying() {
        QueueItem queueItem = waitingQueueItem();
        Playback existing = new Playback(queueItem, Instant.now());
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByIdAndRoomId(QUEUE_ITEM_ID, ROOM_ID)).thenReturn(Optional.of(queueItem));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> playbackService.start(ROOM_ID, QUEUE_ITEM_ID))
                .isInstanceOf(PlaybackAlreadyInProgressException.class);

        verify(playbackRepository, never()).save(any(Playback.class));
        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void startIsTransactional() throws NoSuchMethodException {
        Method method = PlaybackService.class.getMethod("start", Long.class, Long.class);

        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void finishesPlayingPlayback() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        Playback result = playbackService.finish(PLAYBACK_ID);

        assertThat(result).isSameAs(playback);
    }

    @Test
    void playbackStatusChangesToFinished() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        Playback result = playbackService.finish(PLAYBACK_ID);

        assertThat(result.getStatus()).isEqualTo(PlaybackStatus.FINISHED);
    }

    @Test
    void endedAtIsNotNullAfterFinish() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        Playback result = playbackService.finish(PLAYBACK_ID);

        assertThat(result.getEndedAt()).isNotNull();
    }

    @Test
    void queueItemChangesToFinished() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        playbackService.finish(PLAYBACK_ID);

        assertThat(queueItem.getStatus()).isEqualTo(QueueItemStatus.FINISHED);
    }

    @Test
    void throwsWhenPlaybackNotFound() {
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackService.finish(PLAYBACK_ID))
                .isInstanceOf(PlaybackNotFoundException.class);
    }

    @Test
    void throwsWhenPlaybackNotPlaying() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.setStatus(PlaybackStatus.FINISHED);
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.of(playback));

        assertThatThrownBy(() -> playbackService.finish(PLAYBACK_ID))
                .isInstanceOf(PlaybackNotPlayingException.class);

        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void finishIsTransactional() throws NoSuchMethodException {
        Method method = PlaybackService.class.getMethod("finish", Long.class);

        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void skipsPlayingPlayback() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        Playback result = playbackService.skip(PLAYBACK_ID);

        assertThat(result).isSameAs(playback);
    }

    @Test
    void playbackStatusChangesToSkipped() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        Playback result = playbackService.skip(PLAYBACK_ID);

        assertThat(result.getStatus()).isEqualTo(PlaybackStatus.SKIPPED);
    }

    @Test
    void endedAtIsNotNullAfterSkip() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        Playback result = playbackService.skip(PLAYBACK_ID);

        assertThat(result.getEndedAt()).isNotNull();
    }

    @Test
    void queueItemChangesToSkipped() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        playbackService.skip(PLAYBACK_ID);

        assertThat(queueItem.getStatus()).isEqualTo(QueueItemStatus.SKIPPED);
    }

    @Test
    void throwsWhenPlaybackNotFoundForSkip() {
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackService.skip(PLAYBACK_ID))
                .isInstanceOf(PlaybackNotFoundException.class);
    }

    @Test
    void throwsWhenPlaybackNotPlayingForSkip() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.setStatus(PlaybackStatus.FINISHED);
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.of(playback));

        assertThatThrownBy(() -> playbackService.skip(PLAYBACK_ID))
                .isInstanceOf(PlaybackNotPlayingException.class);

        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void skipIsTransactional() throws NoSuchMethodException {
        Method method = PlaybackService.class.getMethod("skip", Long.class);

        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private QueueItem waitingQueueItem() {
        return new QueueItem(room, song, user, Instant.now(), 1);
    }

    private QueueItem playingQueueItem() {
        QueueItem item = new QueueItem(room, song, user, Instant.now(), 1);
        item.setStatus(QueueItemStatus.PLAYING);
        return item;
    }

    private void stubFinishHappyPath(Playback playback) {
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(playbackRepository.save(any(Playback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubHappyPath(QueueItem queueItem) {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByIdAndRoomId(QUEUE_ITEM_ID, ROOM_ID)).thenReturn(Optional.of(queueItem));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playbackRepository.save(any(Playback.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}