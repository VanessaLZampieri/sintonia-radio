package br.com.sintonia.room;

import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomMemberServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @Mock
    private RoomActivityService roomActivityService;

    @InjectMocks
    private RoomMemberService roomMemberService;

    @Test
    void lastMemberLeavingMarksRoomAsEmpty() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        RoomMember member = new RoomMember(room, user);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)).thenReturn(Optional.of(member));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(0L);

        roomMemberService.leaveRoom("abcdefgh", 1L);

        assertThat(member.getLeftAt()).isNotNull();
        assertThat(room.getEmptySince()).isNotNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);

        ArgumentCaptor<RoomActivityType> typeCaptor = ArgumentCaptor.forClass(RoomActivityType.class);
        verify(roomActivityService).record(any(), typeCaptor.capture(), any(), any(), any());
        assertThat(typeCaptor.getValue()).isEqualTo(RoomActivityType.MEMBER_LEFT);
    }

    @Test
    void leavingWithOtherMembersDoesNotMarkRoomAsEmpty() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        RoomMember member = new RoomMember(room, user);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)).thenReturn(Optional.of(member));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(3L);

        roomMemberService.leaveRoom("abcdefgh", 1L);

        assertThat(member.getLeftAt()).isNotNull();
        assertThat(room.getEmptySince()).isNull();
    }

    @Test
    void enteringEmptyRoomClearsEmptySince() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        room.markEmpty();
        User user = mock(User.class);
        when(user.getId()).thenReturn(2L);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)).thenReturn(Optional.empty());
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(0L);
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roomMemberService.enterRoom("abcdefgh", 2L);

        assertThat(room.getEmptySince()).isNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);

        ArgumentCaptor<RoomActivityType> typeCaptor = ArgumentCaptor.forClass(RoomActivityType.class);
        verify(roomActivityService).record(any(), typeCaptor.capture(), any(), any(), any());
        assertThat(typeCaptor.getValue()).isEqualTo(RoomActivityType.MEMBER_JOINED);
    }

    @Test
    void leavingClosedRoomDoesNotMarkEmpty() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        RoomMember member = new RoomMember(room, user);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)).thenReturn(Optional.of(member));

        roomMemberService.leaveRoom("abcdefgh", 1L);

        assertThat(member.getLeftAt()).isNotNull();
        assertThat(room.getEmptySince()).isNull();
    }

    @Test
    void findPresentReturnsPresentMembers() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User ana = user(10L, "Ana Souza", "https://img/ana.jpg");
        User bruno = user(11L, "Bruno Lima", null);
        RoomMember m1 = new RoomMember(room, ana);
        RoomMember m2 = new RoomMember(room, bruno);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.findAllByRoomIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(1L))
                .thenReturn(List.of(m1, m2));
        when(queueItemRepository.countWaitingByUser(1L, QueueItemStatus.WAITING)).thenReturn(List.of());

        List<RoomParticipantResponse> result = roomMemberService.findPresent(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo(10L);
        assertThat(result.get(0).displayName()).isEqualTo("Ana");
        assertThat(result.get(0).avatarUrl()).isEqualTo("https://img/ana.jpg");
        assertThat(result.get(0).waitingCount()).isZero();
        assertThat(result.get(1).userId()).isEqualTo(11L);
        assertThat(result.get(1).displayName()).isEqualTo("Bruno");
        assertThat(result.get(1).avatarUrl()).isNull();
    }

    @Test
    void findPresentIncludesWaitingCount() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User ana = user(10L, "Ana Souza", null);
        RoomMember m1 = new RoomMember(room, ana);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.findAllByRoomIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(1L))
                .thenReturn(List.of(m1));
        when(queueItemRepository.countWaitingByUser(1L, QueueItemStatus.WAITING))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 3L}));

        List<RoomParticipantResponse> result = roomMemberService.findPresent(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).waitingCount()).isEqualTo(3L);
    }

    @Test
    void findPresentReturnsEmptyWhenNoMembers() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.findAllByRoomIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(1L))
                .thenReturn(List.of());
        when(queueItemRepository.countWaitingByUser(1L, QueueItemStatus.WAITING)).thenReturn(List.of());

        assertThat(roomMemberService.findPresent(1L)).isEmpty();
    }

    @Test
    void findPresentRejectsUnknownRoom() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomMemberService.findPresent(1L))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void findPresentRejectsClosedRoom() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        ReflectionTestUtils.setField(room, "id", 1L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomMemberService.findPresent(1L))
                .isInstanceOf(RoomClosedException.class);
    }

    private User user(Long id, String name, String avatarUrl) {
        User user = new User("google-" + id, name, "user" + id + "@example.com", avatarUrl);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
