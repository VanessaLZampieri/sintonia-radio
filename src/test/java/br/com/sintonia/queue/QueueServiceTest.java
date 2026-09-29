package br.com.sintonia.queue;

import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomActivityType;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomMemberRepository;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.song.Song;
import br.com.sintonia.song.SongNotFoundException;
import br.com.sintonia.song.SongDurationLimitExceededException;
import br.com.sintonia.song.SongRepository;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import br.com.sintonia.websocket.QueueChangeAction;
import br.com.sintonia.websocket.QueueChangedEventPayload;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long SONG_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Long QUEUE_ITEM_ID = 42L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    @Mock
    private br.com.sintonia.room.RoomActivityService roomActivityService;

    @InjectMocks
    private QueueService queueService;

    private Room room;
    private Song song;
    private User user;

    @BeforeEach
    void setUp() {
        room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        song = new Song("abc123", "Title", null, Duration.ofSeconds(100));
        user = mock(User.class);
    }

    @Test
    void addsSongToQueue() {
        stubHappyPath(2, 0L);

        QueueItem result = queueService.add(ROOM_ID, SONG_ID, USER_ID);

        assertThat(result.getRoom()).isSameAs(room);
        assertThat(result.getSong()).isSameAs(song);
        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getPosition()).isEqualTo(3);
        assertThat(result.getAddedAt()).isNotNull();
        verify(queueItemRepository).save(any(QueueItem.class));

        ArgumentCaptor<RoomActivityType> typeCaptor = ArgumentCaptor.forClass(RoomActivityType.class);
        verify(roomActivityService).record(any(), typeCaptor.capture(), any(), any(), any());
        assertThat(typeCaptor.getValue()).isEqualTo(RoomActivityType.SONG_ADDED);
    }

    @Test
    void firstSongReceivesPositionOne() {
        stubHappyPath(0, 0L);

        QueueItem result = queueService.add(ROOM_ID, SONG_ID, USER_ID);

        assertThat(result.getPosition()).isEqualTo(1);
    }

    @Test
    void nextSongReceivesFollowingPosition() {
        stubHappyPath(5, 0L);

        QueueItem result = queueService.add(ROOM_ID, SONG_ID, USER_ID);

        assertThat(result.getPosition()).isEqualTo(6);
    }

    @Test
    void addPublishesQueueChangedEvent() {
        stubHappyPath(0, 0L);

        queueService.add(ROOM_ID, SONG_ID, USER_ID);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        RoomEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RoomEventType.QUEUE_CHANGED);
        assertThat(event.roomId()).isEqualTo(ROOM_ID);
        QueueChangedEventPayload payload = (QueueChangedEventPayload) event.payload();
        assertThat(payload.queueItemId()).isEqualTo(QUEUE_ITEM_ID);
        assertThat(payload.action()).isEqualTo(QueueChangeAction.ADDED);
    }

    @Test
    void rejectsWhenRoomDoesNotExist() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(RoomNotFoundException.class);

        verifyNoInteractions(queueItemRepository);
    }

    @Test
    void rejectsWhenRoomIsClosed() {
        Room closedRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(RoomClosedException.class);

        verifyNoInteractions(queueItemRepository);
    }

    @Test
    void rejectsWhenUserIsNotInRoom() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(UserNotInRoomException.class);

        verifyNoInteractions(queueItemRepository);
    }

    @Test
    void rejectsWhenSongDoesNotExist() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(SongNotFoundException.class);

        verifyNoInteractions(queueItemRepository);
    }

    @Test
    void rejectsDuplicateSong() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(queueItemRepository.existsByRoomIdAndSongIdAndStatusIn(eq(ROOM_ID), eq(SONG_ID), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(SongAlreadyInQueueException.class);

        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void duplicateCheckUsesWaitingAndPlayingStatuses() {
        stubHappyPath(0, 0L);

        queueService.add(ROOM_ID, SONG_ID, USER_ID);

        verify(queueItemRepository).existsByRoomIdAndSongIdAndStatusIn(
                eq(ROOM_ID), eq(SONG_ID),
                eq(List.of(QueueItemStatus.WAITING, QueueItemStatus.PLAYING)));
    }

    @Test
    void rejectedAddDoesNotPublishEvent() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(queueItemRepository.existsByRoomIdAndSongIdAndStatusIn(eq(ROOM_ID), eq(SONG_ID), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(SongAlreadyInQueueException.class);

        verifyNoInteractions(roomEventPublisher);
    }

    @Test
    void rejectsWhenUserReachedEightWaitingSongs() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(queueItemRepository.existsByRoomIdAndSongIdAndStatusIn(eq(ROOM_ID), eq(SONG_ID), any()))
                .thenReturn(false);
        when(queueItemRepository.countByRoomIdAndUserIdAndStatus(ROOM_ID, USER_ID, QueueItemStatus.WAITING))
                .thenReturn(8L);

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(QueueLimitExceededException.class);

        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void allowsWhenUserHasSevenWaitingSongs() {
        stubHappyPath(0, 7L);

        QueueItem result = queueService.add(ROOM_ID, SONG_ID, USER_ID);

        assertThat(result).isNotNull();
        verify(queueItemRepository).save(any(QueueItem.class));
    }

    @Test
    void allowsSongWithExactlyTwentyMinutes() {
        song = new Song("twenty", "Twenty", null, Duration.ofMinutes(20));
        stubHappyPath(0, 0L);

        QueueItem result = queueService.add(ROOM_ID, SONG_ID, USER_ID);

        assertThat(result.getSong()).isSameAs(song);
        verify(queueItemRepository).save(any(QueueItem.class));
    }

    @Test
    void rejectsSongLongerThanTwentyMinutesBeforeChangingQueue() {
        song = new Song("long", "Long", null, Duration.ofMinutes(20).plusSeconds(1));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(SongDurationLimitExceededException.class);

        verify(queueItemRepository, never()).save(any());
        verifyNoInteractions(roomEventPublisher, roomActivityService);
    }

    @Test
    void limitIsBasedOnWaitingQueueItems() {
        stubHappyPath(0, 0L);

        queueService.add(ROOM_ID, SONG_ID, USER_ID);

        verify(queueItemRepository).countByRoomIdAndUserIdAndStatus(ROOM_ID, USER_ID, QueueItemStatus.WAITING);
        verify(roomMemberRepository, never()).countByRoomAndLeftAtIsNull(any());
    }

    private void stubHappyPath(int maxPosition, long waitingCount) {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(queueItemRepository.existsByRoomIdAndSongIdAndStatusIn(eq(ROOM_ID), eq(SONG_ID), any()))
                .thenReturn(false);
        when(queueItemRepository.countByRoomIdAndUserIdAndStatus(ROOM_ID, USER_ID, QueueItemStatus.WAITING))
                .thenReturn(waitingCount);
        when(queueItemRepository.findMaxPositionByRoomId(ROOM_ID)).thenReturn(maxPosition);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(invocation -> {
            QueueItem saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", QUEUE_ITEM_ID);
            return saved;
        });
    }

    @Test
    void findQueueThrowsWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.findQueue(ROOM_ID))
                .isInstanceOf(RoomNotFoundException.class);

        verify(queueItemRepository, never())
                .findAllByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }

    @Test
    void findQueueThrowsWhenRoomIsClosed() {
        Room closedRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> queueService.findQueue(ROOM_ID))
                .isInstanceOf(RoomClosedException.class);

        verify(queueItemRepository, never())
                .findAllByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }

    @Test
    void findQueueReturnsItemsWhenRoomIsActive() {
        Room activeRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom));
        List<QueueItem> items = List.of(new QueueItem(activeRoom, song, user, java.time.Instant.now(), 1));
        when(queueItemRepository.findAllByRoomIdAndStatusOrderByPositionAscIdAsc(
                ROOM_ID, QueueItemStatus.WAITING)).thenReturn(items);

        List<QueueItem> result = queueService.findQueue(ROOM_ID);

        assertThat(result).hasSize(1);
        verify(queueItemRepository)
                .findAllByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }

    @Test
    void findQueueReturnsEmptyListWhenRoomIsActiveWithNoItems() {
        Room activeRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom));
        when(queueItemRepository.findAllByRoomIdAndStatusOrderByPositionAscIdAsc(
                ROOM_ID, QueueItemStatus.WAITING)).thenReturn(List.of());

        List<QueueItem> result = queueService.findQueue(ROOM_ID);

        assertThat(result).isEmpty();
        verify(queueItemRepository)
                .findAllByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }

    @Test
    void removeThrowsWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.remove(ROOM_ID, 10L, USER_ID))
                .isInstanceOf(RoomNotFoundException.class);

        verify(queueItemRepository, never()).findByIdAndRoomId(10L, ROOM_ID);
        verify(queueItemRepository, never()).deleteById(10L);
    }

    @Test
    void removeThrowsWhenRoomIsClosed() {
        Room closedRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> queueService.remove(ROOM_ID, 10L, USER_ID))
                .isInstanceOf(RoomClosedException.class);

        verify(queueItemRepository, never()).findByIdAndRoomId(10L, ROOM_ID);
        verify(queueItemRepository, never()).deleteById(10L);
    }

    @Test
    void removeThrowsWhenUserNotInRoom() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> queueService.remove(ROOM_ID, 10L, USER_ID))
                .isInstanceOf(UserNotInRoomException.class);

        verify(queueItemRepository, never()).findByIdAndRoomId(10L, ROOM_ID);
        verify(queueItemRepository, never()).deleteById(10L);
    }

    @Test
    void removeThrowsWhenItemNotFound() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(queueItemRepository.findByIdAndRoomId(10L, ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.remove(ROOM_ID, 10L, USER_ID))
                .isInstanceOf(QueueItemNotFoundException.class);

        verify(queueItemRepository).findByIdAndRoomId(10L, ROOM_ID);
        verify(queueItemRepository, never()).deleteById(10L);
    }

    @Test
    void removeDeletesItemWhenAllValidationsPass() {
        QueueItem item = new QueueItem(room, song, user, java.time.Instant.now(), 1);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(queueItemRepository.findByIdAndRoomId(10L, ROOM_ID)).thenReturn(Optional.of(item));
        when(user.getId()).thenReturn(USER_ID);

        queueService.remove(ROOM_ID, 10L, USER_ID);

        verify(queueItemRepository).findByIdAndRoomId(10L, ROOM_ID);
        verify(queueItemRepository).deleteById(10L);

        ArgumentCaptor<RoomActivityType> typeCaptor = ArgumentCaptor.forClass(RoomActivityType.class);
        verify(roomActivityService).record(any(), typeCaptor.capture(), any(), any(), any());
        assertThat(typeCaptor.getValue()).isEqualTo(RoomActivityType.SONG_REMOVED);
    }

    @Test
    void removePublishesQueueChangedEvent() {
        QueueItem item = new QueueItem(room, song, user, java.time.Instant.now(), 1);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(queueItemRepository.findByIdAndRoomId(10L, ROOM_ID)).thenReturn(Optional.of(item));
        when(user.getId()).thenReturn(USER_ID);

        queueService.remove(ROOM_ID, 10L, USER_ID);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        RoomEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RoomEventType.QUEUE_CHANGED);
        assertThat(event.roomId()).isEqualTo(ROOM_ID);
        QueueChangedEventPayload payload = (QueueChangedEventPayload) event.payload();
        assertThat(payload.queueItemId()).isEqualTo(10L);
        assertThat(payload.action()).isEqualTo(QueueChangeAction.REMOVED);
    }

    @Test
    void removeRejectsItemAddedByAnotherUser() {
        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(999L);
        QueueItem item = new QueueItem(room, song, otherUser, java.time.Instant.now(), 1);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(queueItemRepository.findByIdAndRoomId(10L, ROOM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> queueService.remove(ROOM_ID, 10L, USER_ID))
                .isInstanceOf(QueueItemNotOwnedException.class);

        verify(queueItemRepository, never()).deleteById(10L);
        verifyNoInteractions(roomEventPublisher, roomActivityService);
    }

    @Test
    void removeRejectsOwnItemThatIsNotWaiting() {
        QueueItem item = new QueueItem(room, song, user, java.time.Instant.now(), 1);
        item.setStatus(QueueItemStatus.PLAYING);
        when(user.getId()).thenReturn(USER_ID);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(queueItemRepository.findByIdAndRoomId(10L, ROOM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> queueService.remove(ROOM_ID, 10L, USER_ID))
                .isInstanceOf(br.com.sintonia.playback.QueueItemNotWaitingException.class);

        verify(queueItemRepository, never()).deleteById(10L);
        verifyNoInteractions(roomEventPublisher, roomActivityService);
    }

    @Test
    void findByIdReturnsItemWhenFound() {
        QueueItem item = new QueueItem(room, song, user, java.time.Instant.now(), 1);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByIdAndRoomId(10L, ROOM_ID)).thenReturn(Optional.of(item));

        QueueItem result = queueService.findById(ROOM_ID, 10L);

        assertThat(result).isSameAs(item);
        verify(queueItemRepository).findByIdAndRoomId(10L, ROOM_ID);
    }

    @Test
    void findByIdThrowsWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.findById(ROOM_ID, 10L))
                .isInstanceOf(RoomNotFoundException.class);

        verify(queueItemRepository, never()).findByIdAndRoomId(10L, ROOM_ID);
    }

    @Test
    void findByIdThrowsWhenRoomIsClosed() {
        Room closedRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> queueService.findById(ROOM_ID, 10L))
                .isInstanceOf(RoomClosedException.class);

        verify(queueItemRepository, never()).findByIdAndRoomId(10L, ROOM_ID);
    }

    @Test
    void findByIdThrowsWhenItemNotFound() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByIdAndRoomId(10L, ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.findById(ROOM_ID, 10L))
                .isInstanceOf(QueueItemNotFoundException.class);

        verify(queueItemRepository).findByIdAndRoomId(10L, ROOM_ID);
    }

    @Test
    void findByIdThrowsWhenItemBelongsToOtherRoom() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByIdAndRoomId(10L, ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.findById(ROOM_ID, 10L))
                .isInstanceOf(QueueItemNotFoundException.class);

        verify(queueItemRepository).findByIdAndRoomId(10L, ROOM_ID);
    }

    @Test
    void findPlayingReturnsItemWhenRoomHasPlaying() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        QueueItem playing = new QueueItem(room, song, user, java.time.Instant.now(), 1);
        when(queueItemRepository.findByRoomIdAndStatus(ROOM_ID, QueueItemStatus.PLAYING))
                .thenReturn(Optional.of(playing));

        Optional<QueueItem> result = queueService.findPlaying(ROOM_ID);

        assertThat(result).contains(playing);
        verify(queueItemRepository).findByRoomIdAndStatus(ROOM_ID, QueueItemStatus.PLAYING);
    }

    @Test
    void findPlayingReturnsEmptyWhenNoPlaying() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findByRoomIdAndStatus(ROOM_ID, QueueItemStatus.PLAYING))
                .thenReturn(Optional.empty());

        Optional<QueueItem> result = queueService.findPlaying(ROOM_ID);

        assertThat(result).isEmpty();
        verify(queueItemRepository).findByRoomIdAndStatus(ROOM_ID, QueueItemStatus.PLAYING);
    }

    @Test
    void findPlayingThrowsWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.findPlaying(ROOM_ID))
                .isInstanceOf(RoomNotFoundException.class);

        verify(queueItemRepository, never()).findByRoomIdAndStatus(ROOM_ID, QueueItemStatus.PLAYING);
    }

    @Test
    void findPlayingThrowsWhenRoomIsClosed() {
        Room closedRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> queueService.findPlaying(ROOM_ID))
                .isInstanceOf(RoomClosedException.class);

        verify(queueItemRepository, never()).findByRoomIdAndStatus(ROOM_ID, QueueItemStatus.PLAYING);
    }

    @Test
    void findNextWaitingReturnsItemWhenRoomHasWaiting() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        QueueItem waiting = new QueueItem(room, song, user, java.time.Instant.now(), 1);
        when(queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING))
                .thenReturn(Optional.of(waiting));

        Optional<QueueItem> result = queueService.findNextWaiting(ROOM_ID);

        assertThat(result).contains(waiting);
        verify(queueItemRepository).findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }

    @Test
    void findNextWaitingReturnsEmptyWhenNoWaiting() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING))
                .thenReturn(Optional.empty());

        Optional<QueueItem> result = queueService.findNextWaiting(ROOM_ID);

        assertThat(result).isEmpty();
        verify(queueItemRepository).findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }

    @Test
    void findNextWaitingThrowsWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.findNextWaiting(ROOM_ID))
                .isInstanceOf(RoomNotFoundException.class);

        verify(queueItemRepository, never())
                .findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }

    @Test
    void findNextWaitingThrowsWhenRoomIsClosed() {
        Room closedRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> queueService.findNextWaiting(ROOM_ID))
                .isInstanceOf(RoomClosedException.class);

        verify(queueItemRepository, never())
                .findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }
}
