package br.com.sintonia.queue;

import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomMemberRepository;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.song.Song;
import br.com.sintonia.song.SongNotFoundException;
import br.com.sintonia.song.SongRepository;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private QueueService queueService;

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
    void addsSongToQueue() {
        stubHappyPath(2, 0L);

        QueueItem result = queueService.add(ROOM_ID, SONG_ID, USER_ID);

        assertThat(result.getRoom()).isSameAs(room);
        assertThat(result.getSong()).isSameAs(song);
        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getPosition()).isEqualTo(3);
        assertThat(result.getAddedAt()).isNotNull();
        verify(queueItemRepository).save(any(QueueItem.class));
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
    void rejectsWhenRoomDoesNotExist() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(RoomNotFoundException.class);

        verifyNoInteractions(queueItemRepository);
    }

    @Test
    void rejectsWhenRoomIsClosed() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
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
        when(queueItemRepository.existsByRoomIdAndSongId(ROOM_ID, SONG_ID)).thenReturn(true);

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(SongAlreadyInQueueException.class);

        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void rejectsWhenUserReachedEightSongs() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(queueItemRepository.existsByRoomIdAndSongId(ROOM_ID, SONG_ID)).thenReturn(false);
        when(queueItemRepository.countByRoomIdAndUserId(ROOM_ID, USER_ID)).thenReturn(8L);

        assertThatThrownBy(() -> queueService.add(ROOM_ID, SONG_ID, USER_ID))
                .isInstanceOf(QueueLimitExceededException.class);

        verify(queueItemRepository, never()).save(any(QueueItem.class));
    }

    @Test
    void allowsWhenUserHasSevenSongs() {
        stubHappyPath(0, 7L);

        QueueItem result = queueService.add(ROOM_ID, SONG_ID, USER_ID);

        assertThat(result).isNotNull();
        verify(queueItemRepository).save(any(QueueItem.class));
    }

    @Test
    void limitIsBasedOnQueueItemsNotMembership() {
        stubHappyPath(0, 0L);

        queueService.add(ROOM_ID, SONG_ID, USER_ID);

        verify(queueItemRepository).countByRoomIdAndUserId(ROOM_ID, USER_ID);
        verify(roomMemberRepository, never()).countByRoomAndLeftAtIsNull(any());
    }

    private void stubHappyPath(int maxPosition, long userCount) {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(queueItemRepository.existsByRoomIdAndSongId(ROOM_ID, SONG_ID)).thenReturn(false);
        when(queueItemRepository.countByRoomIdAndUserId(ROOM_ID, USER_ID)).thenReturn(userCount);
        when(queueItemRepository.findMaxPositionByRoomId(ROOM_ID)).thenReturn(maxPosition);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void findQueueThrowsWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.findQueue(ROOM_ID))
                .isInstanceOf(RoomNotFoundException.class);

        verify(queueItemRepository, never()).findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID);
    }

    @Test
    void findQueueThrowsWhenRoomIsClosed() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> queueService.findQueue(ROOM_ID))
                .isInstanceOf(RoomClosedException.class);

        verify(queueItemRepository, never()).findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID);
    }

    @Test
    void findQueueReturnsItemsWhenRoomIsActive() {
        Room activeRoom = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom));
        List<QueueItem> items = List.of(new QueueItem(activeRoom, song, user, java.time.Instant.now(), 1));
        when(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID)).thenReturn(items);

        List<QueueItem> result = queueService.findQueue(ROOM_ID);

        assertThat(result).hasSize(1);
        verify(queueItemRepository).findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID);
    }

    @Test
    void findQueueReturnsEmptyListWhenRoomIsActiveWithNoItems() {
        Room activeRoom = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom));
        when(queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID)).thenReturn(List.of());

        List<QueueItem> result = queueService.findQueue(ROOM_ID);

        assertThat(result).isEmpty();
        verify(queueItemRepository).findAllByRoomIdOrderByPositionAscIdAsc(ROOM_ID);
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
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
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

        queueService.remove(ROOM_ID, 10L, USER_ID);

        verify(queueItemRepository).findByIdAndRoomId(10L, ROOM_ID);
        verify(queueItemRepository).deleteById(10L);
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
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
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
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
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
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> queueService.findNextWaiting(ROOM_ID))
                .isInstanceOf(RoomClosedException.class);

        verify(queueItemRepository, never())
                .findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING);
    }
}
