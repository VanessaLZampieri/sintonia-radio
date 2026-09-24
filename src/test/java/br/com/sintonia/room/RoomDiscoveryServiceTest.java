package br.com.sintonia.room;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomDiscoveryServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @InjectMocks
    private RoomDiscoveryService roomDiscoveryService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
    }

    @Test
    void listActiveRoomsReturnsCountsAndNowPlaying() {
        when(roomRepository.findActiveRoomsWithPresentMembers(RoomStatus.ACTIVE)).thenReturn(List.of(room));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(3L);
        when(queueItemRepository.countByRoomIdAndStatus(ROOM_ID, QueueItemStatus.WAITING)).thenReturn(2L);

        Song song = new Song("vid", "Título", "https://img.jpg", Duration.ofSeconds(200));
        User user = user(10L, "Ana Souza", "https://img/ana.jpg");
        QueueItem playing = new QueueItem(room, song, user, Instant.now(), 1);
        when(queueItemRepository.findByRoomIdAndStatus(ROOM_ID, QueueItemStatus.PLAYING))
                .thenReturn(Optional.of(playing));

        List<ActiveRoomResponse> result = roomDiscoveryService.listActiveRooms();

        assertThat(result).hasSize(1);
        ActiveRoomResponse item = result.get(0);
        assertThat(item.roomId()).isEqualTo(ROOM_ID);
        assertThat(item.name()).isEqualTo("Sala Teste");
        assertThat(item.code()).isEqualTo("ABCDEFGH");
        assertThat(item.participantCount()).isEqualTo(3L);
        assertThat(item.waitingCount()).isEqualTo(2L);
        assertThat(item.nowPlaying()).isNotNull();
        assertThat(item.nowPlaying().title()).isEqualTo("Título");
        assertThat(item.nowPlaying().youtubeVideoId()).isEqualTo("vid");
        assertThat(item.nowPlaying().thumbnailUrl()).isEqualTo("https://img.jpg");
        assertThat(item.nowPlaying().addedBy()).isNotNull();
        assertThat(item.nowPlaying().addedBy().userId()).isEqualTo(10L);
        assertThat(item.nowPlaying().addedBy().displayName()).isEqualTo("Ana");
    }

    @Test
    void listActiveRoomsNowPlayingNullWhenNoPlayingItem() {
        when(roomRepository.findActiveRoomsWithPresentMembers(RoomStatus.ACTIVE)).thenReturn(List.of(room));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(1L);
        when(queueItemRepository.countByRoomIdAndStatus(ROOM_ID, QueueItemStatus.WAITING)).thenReturn(0L);
        when(queueItemRepository.findByRoomIdAndStatus(ROOM_ID, QueueItemStatus.PLAYING))
                .thenReturn(Optional.empty());

        List<ActiveRoomResponse> result = roomDiscoveryService.listActiveRooms();

        assertThat(result.get(0).nowPlaying()).isNull();
    }

    @Test
    void listUserRoomsDeduplicatesAndKeepsMostRecentOrder() {
        Room roomA = room(1L, "Sala A");
        Room roomB = room(2L, "Sala B");
        User user = user(USER_ID, "Ana Souza", null);

        when(roomMemberRepository.findByUserIdAndRoomStatusOrderByJoinedAtDesc(USER_ID, RoomStatus.ACTIVE))
                .thenReturn(List.of(
                        new RoomMember(roomA, user),
                        new RoomMember(roomB, user),
                        new RoomMember(roomA, user)));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(roomA)).thenReturn(1L);
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(roomB)).thenReturn(1L);

        List<UserRoomResponse> result = roomDiscoveryService.listUserRooms(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).roomId()).isEqualTo(1L);
        assertThat(result.get(1).roomId()).isEqualTo(2L);
    }

    @Test
    void listUserRoomsDoesNotReturnClosedRooms() {
        when(roomMemberRepository.findByUserIdAndRoomStatusOrderByJoinedAtDesc(USER_ID, RoomStatus.ACTIVE))
                .thenReturn(List.of());

        List<UserRoomResponse> result = roomDiscoveryService.listUserRooms(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void listUserRoomsActiveNotFullCanEnter() {
        User user = user(USER_ID, "Ana Souza", null);
        when(roomMemberRepository.findByUserIdAndRoomStatusOrderByJoinedAtDesc(USER_ID, RoomStatus.ACTIVE))
                .thenReturn(List.of(new RoomMember(room, user)));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(3L);

        List<UserRoomResponse> result = roomDiscoveryService.listUserRooms(USER_ID);

        assertThat(result.get(0).canEnter()).isTrue();
    }

    @Test
    void listUserRoomsActiveFullCannotEnter() {
        User user = user(USER_ID, "Ana Souza", null);
        when(roomMemberRepository.findByUserIdAndRoomStatusOrderByJoinedAtDesc(USER_ID, RoomStatus.ACTIVE))
                .thenReturn(List.of(new RoomMember(room, user)));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(15L);

        List<UserRoomResponse> result = roomDiscoveryService.listUserRooms(USER_ID);

        assertThat(result.get(0).canEnter()).isFalse();
    }

    private Room room(Long id, String name) {
        Room r = new Room(name, "CODE" + id, RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    private User user(Long id, String name, String avatarUrl) {
        User u = new User("google-" + id, name, "user" + id + "@example.com", avatarUrl);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}
