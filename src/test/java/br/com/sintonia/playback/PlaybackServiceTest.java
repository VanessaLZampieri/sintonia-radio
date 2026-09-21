package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemNotFoundException;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.queue.QueueService;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.NotThePlayerException;
import br.com.sintonia.room.PlaybackMode;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomMemberRepository;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import br.com.sintonia.websocket.PlaybackEventPayload;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import br.com.sintonia.websocket.RoomEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaybackServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long QUEUE_ITEM_ID = 42L;
    private static final Long PLAYBACK_ID = 101L;
    private static final Long USER_ID = 456L;
    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @Mock
    private PlaybackRepository playbackRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private QueueService queueService;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    @Mock
    private AutoDjService autoDjService;

    @InjectMocks
    private PlaybackService playbackService;

    private Room room;
    private Song song;
    private User user;

    @BeforeEach
    void setUp() {
        room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
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

        Playback result = playbackService.finish(PLAYBACK_ID, USER_ID, null);

        assertThat(result).isSameAs(playback);
    }

    @Test
    void playbackStatusChangesToFinished() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        Playback result = playbackService.finish(PLAYBACK_ID, USER_ID, null);

        assertThat(result.getStatus()).isEqualTo(PlaybackStatus.FINISHED);
    }

    @Test
    void endedAtIsNotNullAfterFinish() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        Playback result = playbackService.finish(PLAYBACK_ID, USER_ID, null);

        assertThat(result.getEndedAt()).isNotNull();
    }

    @Test
    void queueItemChangesToFinished() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        playbackService.finish(PLAYBACK_ID, USER_ID, null);

        assertThat(queueItem.getStatus()).isEqualTo(QueueItemStatus.FINISHED);
    }

    @Test
    void throwsWhenPlaybackNotFound() {
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackService.finish(PLAYBACK_ID, USER_ID, null))
                .isInstanceOf(PlaybackNotFoundException.class);
    }

    @Test
    void throwsWhenPlaybackNotPlaying() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.setStatus(PlaybackStatus.FINISHED);
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));

        assertThatThrownBy(() -> playbackService.finish(PLAYBACK_ID, USER_ID, null))
                .isInstanceOf(PlaybackNotPlayingException.class);

        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void finishIsTransactional() throws NoSuchMethodException {
        Method method = PlaybackService.class.getMethod("finish", Long.class, Long.class, String.class);

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
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackService.skip(PLAYBACK_ID))
                .isInstanceOf(PlaybackNotFoundException.class);
    }

    @Test
    void throwsWhenPlaybackNotPlayingForSkip() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.setStatus(PlaybackStatus.FINISHED);
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));

        assertThatThrownBy(() -> playbackService.skip(PLAYBACK_ID))
                .isInstanceOf(PlaybackNotPlayingException.class);

        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void skipIsTransactional() throws NoSuchMethodException {
        Method method = PlaybackService.class.getMethod("skip", Long.class);

        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void finishTriggersFindNextWaiting() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        playbackService.finish(PLAYBACK_ID, USER_ID, null);

        verify(queueService).findNextWaiting(ROOM_ID);
    }

    @Test
    void finishStartsNextPlaybackWhenWaitingItemExists() {
        QueueItem current = playingQueueItem();
        Playback playback = new Playback(current, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);

        QueueItem nextRef = mock(QueueItem.class);
        when(nextRef.getId()).thenReturn(QUEUE_ITEM_ID);
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.of(nextRef));

        QueueItem actualNext = waitingQueueItem();
        stubHappyPath(actualNext);

        playbackService.finish(PLAYBACK_ID, USER_ID, null);

        assertThat(current.getStatus()).isEqualTo(QueueItemStatus.FINISHED);
        assertThat(actualNext.getStatus()).isEqualTo(QueueItemStatus.PLAYING);
    }

    @Test
    void skipStartsNextPlaybackWhenWaitingItemExists() {
        QueueItem current = playingQueueItem();
        Playback playback = new Playback(current, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));

        QueueItem nextRef = mock(QueueItem.class);
        when(nextRef.getId()).thenReturn(QUEUE_ITEM_ID);
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.of(nextRef));

        QueueItem actualNext = waitingQueueItem();
        stubHappyPath(actualNext);

        playbackService.skip(PLAYBACK_ID);

        assertThat(current.getStatus()).isEqualTo(QueueItemStatus.SKIPPED);
        assertThat(actualNext.getStatus()).isEqualTo(QueueItemStatus.PLAYING);
    }

    @Test
    void startNextThrowsWhenRoomDoesNotExist() {
        when(queueService.findNextWaiting(ROOM_ID)).thenThrow(new RoomNotFoundException("Sala não encontrada."));

        assertThatThrownBy(() -> playbackService.startNext(ROOM_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void startNextThrowsWhenRoomIsClosed() {
        when(queueService.findNextWaiting(ROOM_ID)).thenThrow(new RoomClosedException("Esta sala foi encerrada."));

        assertThatThrownBy(() -> playbackService.startNext(ROOM_ID))
                .isInstanceOf(RoomClosedException.class);
    }

    @Test
    void startNextReturnsEmptyWhenNoWaitingQueueItem() {
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.empty());

        Optional<Playback> result = playbackService.startNext(ROOM_ID);

        assertThat(result).isEmpty();
        verify(playbackRepository, never()).save(any(Playback.class));
    }

    @Test
    void startNextCreatesPlaybackWhenWaitingQueueItemExists() {
        stubStartNextHappyPath();

        Optional<Playback> result = playbackService.startNext(ROOM_ID);

        assertThat(result).isPresent();
    }

    @Test
    void startNextUsesReturnedQueueItem() {
        stubStartNextHappyPath();

        playbackService.startNext(ROOM_ID);

        verify(queueItemRepository).findByIdAndRoomId(QUEUE_ITEM_ID, ROOM_ID);
    }

    @Test
    void startNextChangesQueueItemToPlaying() {
        QueueItem queueItem = stubStartNextHappyPath();

        playbackService.startNext(ROOM_ID);

        assertThat(queueItem.getStatus()).isEqualTo(QueueItemStatus.PLAYING);
    }

    @Test
    void startNextCreatesPlayingPlayback() {
        stubStartNextHappyPath();

        Optional<Playback> result = playbackService.startNext(ROOM_ID);

        assertThat(result.get().getStatus()).isEqualTo(PlaybackStatus.PLAYING);
    }

    @Test
    void startNextPlaybackAssociatedWithCorrectQueueItem() {
        QueueItem queueItem = stubStartNextHappyPath();

        Optional<Playback> result = playbackService.startNext(ROOM_ID);

        assertThat(result.get().getQueueItem()).isSameAs(queueItem);
    }

    @Test
    void startNextCreatesOnlyOnePlayback() {
        stubStartNextHappyPath();

        playbackService.startNext(ROOM_ID);

        verify(playbackRepository, times(1)).save(any(Playback.class));
        verify(queueItemRepository, times(1)).save(any(QueueItem.class));
    }

    @Test
    void startPublishesPlaybackStartedEvent() {
        QueueItem queueItem = waitingQueueItem();
        stubHappyPath(queueItem);

        playbackService.start(ROOM_ID, QUEUE_ITEM_ID);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        RoomEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RoomEventType.PLAYBACK_STARTED);
        assertThat(event.roomId()).isEqualTo(ROOM_ID);
        PlaybackEventPayload payload = (PlaybackEventPayload) event.payload();
        assertThat(payload.playbackId()).isEqualTo(PLAYBACK_ID);
        assertThat(payload.queueItemId()).isEqualTo(QUEUE_ITEM_ID);
    }

    @Test
    void finishPublishesPlaybackFinishedEvent() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        playbackService.finish(PLAYBACK_ID, USER_ID, null);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        RoomEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RoomEventType.PLAYBACK_FINISHED);
        assertThat(event.roomId()).isEqualTo(ROOM_ID);
    }

    @Test
    void skipPublishesPlaybackSkippedEvent() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        playbackService.skip(PLAYBACK_ID);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        RoomEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RoomEventType.PLAYBACK_SKIPPED);
        assertThat(event.roomId()).isEqualTo(ROOM_ID);
    }

    @Test
    void finishWithNextItemPublishesFinishedThenStarted() {
        QueueItem current = playingQueueItem();
        Playback playback = new Playback(current, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);

        QueueItem nextRef = mock(QueueItem.class);
        when(nextRef.getId()).thenReturn(QUEUE_ITEM_ID);
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.of(nextRef));

        QueueItem actualNext = waitingQueueItem();
        stubHappyPath(actualNext);

        playbackService.finish(PLAYBACK_ID, USER_ID, null);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher, times(2)).publish(eq("ABCDEFGH"), captor.capture());
        var events = captor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).eventType()).isEqualTo(RoomEventType.PLAYBACK_FINISHED);
        assertThat(events.get(1).eventType()).isEqualTo(RoomEventType.PLAYBACK_STARTED);
    }

    @Test
    void skipWithNextItemPublishesSkippedThenStarted() {
        QueueItem current = playingQueueItem();
        Playback playback = new Playback(current, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));

        QueueItem nextRef = mock(QueueItem.class);
        when(nextRef.getId()).thenReturn(QUEUE_ITEM_ID);
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.of(nextRef));

        QueueItem actualNext = waitingQueueItem();
        stubHappyPath(actualNext);

        playbackService.skip(PLAYBACK_ID);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher, times(2)).publish(eq("ABCDEFGH"), captor.capture());
        var events = captor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).eventType()).isEqualTo(RoomEventType.PLAYBACK_SKIPPED);
        assertThat(events.get(1).eventType()).isEqualTo(RoomEventType.PLAYBACK_STARTED);
    }

    @Test
    void startNextInvokesAutoDjWhenQueueEmpty() {
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.empty());
        QueueItem autoItem = waitingQueueItem();
        when(autoDjService.createNext(ROOM_ID)).thenReturn(Optional.of(autoItem));
        stubHappyPath(autoItem);

        playbackService.startNext(ROOM_ID);

        verify(autoDjService).createNext(ROOM_ID);
        verify(queueItemRepository).findByIdAndRoomId(QUEUE_ITEM_ID, ROOM_ID);
    }

    @Test
    void ensurePlaybackStartsWaitingItemWhenIdle() {
        QueueItem nextRef = mock(QueueItem.class);
        when(nextRef.getId()).thenReturn(QUEUE_ITEM_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.of(nextRef));
        QueueItem actualNext = waitingQueueItem();
        stubHappyPath(actualNext);

        Optional<Playback> result = playbackService.ensurePlayback(ROOM_ID);

        assertThat(result).isPresent();
        assertThat(actualNext.getStatus()).isEqualTo(QueueItemStatus.PLAYING);
        verify(playbackRepository).save(any(Playback.class));
    }

    @Test
    void ensurePlaybackDoesNothingWhenAlreadyPlaying() {
        QueueItem queueItem = waitingQueueItem();
        Playback existing = new Playback(queueItem, Instant.now());
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.of(existing));

        Optional<Playback> result = playbackService.ensurePlayback(ROOM_ID);

        assertThat(result).isEmpty();
        verify(queueService, never()).findNextWaiting(ROOM_ID);
        verify(playbackRepository, never()).save(any(Playback.class));
        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void ensurePlaybackDoesNothingWhenNoWaitingItem() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.empty());

        Optional<Playback> result = playbackService.ensurePlayback(ROOM_ID);

        assertThat(result).isEmpty();
        verify(playbackRepository, never()).save(any(Playback.class));
    }

    @Test
    void ensurePlaybackNeverCreatesAutoDj() {
        QueueItem nextRef = mock(QueueItem.class);
        when(nextRef.getId()).thenReturn(QUEUE_ITEM_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.of(nextRef));
        QueueItem actualNext = waitingQueueItem();
        stubHappyPath(actualNext);

        playbackService.ensurePlayback(ROOM_ID);

        verify(autoDjService, never()).createNext(any());
    }

    @Test
    void ensurePlaybackThrowsWhenRoomDoesNotExist() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackService.ensurePlayback(ROOM_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void ensurePlaybackThrowsWhenRoomIsClosed() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> playbackService.ensurePlayback(ROOM_ID))
                .isInstanceOf(RoomClosedException.class);
    }

    @Test
    void ensurePlaybackIsTransactional() throws NoSuchMethodException {
        Method method = PlaybackService.class.getMethod("ensurePlayback", Long.class);

        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void errorMarksPlaybackError() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        playbackService.error(PLAYBACK_ID, USER_ID, null);

        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.ERROR);
        assertThat(playback.getEndedAt()).isNotNull();
        assertThat(queueItem.getStatus()).isEqualTo(QueueItemStatus.ERROR);
    }

    @Test
    void errorPublishesPlaybackErrorEvent() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubFinishHappyPath(playback);

        playbackService.error(PLAYBACK_ID, USER_ID, null);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RoomEventType.PLAYBACK_ERROR);
    }

    @Test
    void errorThrowsWhenPlaybackNotFound() {
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playbackService.error(PLAYBACK_ID, USER_ID, null))
                .isInstanceOf(PlaybackNotFoundException.class);
    }

    @Test
    void errorThrowsWhenPlaybackNotPlaying() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.setStatus(PlaybackStatus.FINISHED);
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));

        assertThatThrownBy(() -> playbackService.error(PLAYBACK_ID, USER_ID, null))
                .isInstanceOf(PlaybackNotPlayingException.class);
    }

    @Test
    void pausesPlayingPlayback() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubPauseHappyPath(playback);

        Playback result = playbackService.pause(PLAYBACK_ID, USER_ID, null);

        assertThat(result.isPaused()).isTrue();
    }

    @Test
    void pauseIsIdempotent() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.pause();
        stubPauseHappyPath(playback);

        playbackService.pause(PLAYBACK_ID, USER_ID, null);

        verify(roomEventPublisher, never()).publish(eq("ABCDEFGH"), any(RoomEvent.class));
    }

    @Test
    void resumesPausedPlayback() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.pause();
        stubPauseHappyPath(playback);

        Playback result = playbackService.resume(PLAYBACK_ID, USER_ID, null);

        assertThat(result.isPaused()).isFalse();
    }

    @Test
    void resumeWhenNotPausedIsIdempotent() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubPauseHappyPath(playback);

        playbackService.resume(PLAYBACK_ID, USER_ID, null);

        verify(roomEventPublisher, never()).publish(eq("ABCDEFGH"), any(RoomEvent.class));
    }

    @Test
    void pausePublishesPausedEvent() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        stubPauseHappyPath(playback);

        playbackService.pause(PLAYBACK_ID, USER_ID, null);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RoomEventType.PLAYBACK_PAUSED);
    }

    @Test
    void resumePublishesResumedEvent() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.pause();
        stubPauseHappyPath(playback);

        playbackService.resume(PLAYBACK_ID, USER_ID, null);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RoomEventType.PLAYBACK_RESUMED);
    }

    @Test
    void pauseRejectsNonMember() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> playbackService.pause(PLAYBACK_ID, USER_ID, null))
                .isInstanceOf(UserNotInRoomException.class);
    }

    @Test
    void pauseRejectsInTodosOsNavegadores() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> playbackService.pause(PLAYBACK_ID, USER_ID, null))
                .isInstanceOf(PlaybackCommandNotAllowedException.class);

        verify(playbackRepository, never()).save(any(Playback.class));
    }

    @Test
    void resumeRejectsInTodosOsNavegadores() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        playback.pause();
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> playbackService.resume(PLAYBACK_ID, USER_ID, null))
                .isInstanceOf(PlaybackCommandNotAllowedException.class);

        verify(playbackRepository, never()).save(any(Playback.class));
    }

    @Test
    void pauseRejectsWrongPlayerSessionInCaixaDeMusica() {
        room.changePlaybackMode(PlaybackMode.CAIXA_DE_MUSICA);
        room.claim(SESSION_ID, user);
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> playbackService.pause(PLAYBACK_ID, USER_ID, "other-session"))
                .isInstanceOf(NotThePlayerException.class);
    }

    @Test
    void pauseAcceptsPlayerSessionInCaixaDeMusica() {
        room.changePlaybackMode(PlaybackMode.CAIXA_DE_MUSICA);
        room.claim(SESSION_ID, user);
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(playbackRepository.save(any(Playback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        playbackService.pause(PLAYBACK_ID, USER_ID, SESSION_ID);

        assertThat(playback.isPaused()).isTrue();
    }

    @Test
    void finishRejectsNonMember() {
        QueueItem queueItem = playingQueueItem();
        Playback playback = new Playback(queueItem, Instant.now());
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> playbackService.finish(PLAYBACK_ID, USER_ID, null))
                .isInstanceOf(UserNotInRoomException.class);
    }

    private void stubPauseHappyPath(Playback playback) {
        room.changePlaybackMode(PlaybackMode.CAIXA_DE_MUSICA);
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(playbackRepository.save(any(Playback.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private QueueItem stubStartNextHappyPath() {
        QueueItem nextQueueItem = mock(QueueItem.class);
        when(nextQueueItem.getId()).thenReturn(QUEUE_ITEM_ID);
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.of(nextQueueItem));
        QueueItem queueItem = waitingQueueItem();
        stubHappyPath(queueItem);
        return queueItem;
    }

    private QueueItem waitingQueueItem() {
        QueueItem item = new QueueItem(room, song, user, Instant.now(), 1);
        ReflectionTestUtils.setField(item, "id", QUEUE_ITEM_ID);
        return item;
    }

    private QueueItem playingQueueItem() {
        QueueItem item = new QueueItem(room, song, user, Instant.now(), 1);
        item.setStatus(QueueItemStatus.PLAYING);
        ReflectionTestUtils.setField(item, "id", QUEUE_ITEM_ID);
        return item;
    }

    private void stubFinishHappyPath(Playback playback) {
        when(playbackRepository.findByIdForUpdate(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(playbackRepository.save(any(Playback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queueService.findNextWaiting(ROOM_ID)).thenReturn(Optional.empty());
    }

    private void stubHappyPath(QueueItem queueItem) {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByIdAndRoomId(QUEUE_ITEM_ID, ROOM_ID)).thenReturn(Optional.of(queueItem));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playbackRepository.save(any(Playback.class))).thenAnswer(invocation -> {
            Playback saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", PLAYBACK_ID);
            return saved;
        });
    }
}