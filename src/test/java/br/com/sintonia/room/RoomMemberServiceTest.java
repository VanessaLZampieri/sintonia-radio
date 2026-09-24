package br.com.sintonia.room;

import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import br.com.sintonia.websocket.RoomEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
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
class RoomMemberServiceTest {

    private static final String CLIENT_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private RoomPresenceRepository roomPresenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @Mock
    private RoomActivityService roomActivityService;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    @Mock
    private RoomPlayerService roomPlayerService;

    @InjectMocks
    private RoomMemberService roomMemberService;

    @Test
    void lastMemberLeavingMarksRoomAsEmpty() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        RoomMember member = new RoomMember(room, user);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        RoomPresence presence = new RoomPresence(room, user, member, CLIENT_ID,
                java.time.Instant.now().plusSeconds(90));
        when(roomPresenceRepository.findByRoomAndUserAndClientSessionId(room, user, CLIENT_ID))
                .thenReturn(Optional.of(presence));
        when(roomPresenceRepository.countPresentUsers(any(), any())).thenReturn(0L);

        roomMemberService.leaveRoom("abcdefgh", 1L, CLIENT_ID);

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
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        RoomMember member = new RoomMember(room, user);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        RoomPresence presence = new RoomPresence(room, user, member, CLIENT_ID,
                java.time.Instant.now().plusSeconds(90));
        when(roomPresenceRepository.findByRoomAndUserAndClientSessionId(room, user, CLIENT_ID))
                .thenReturn(Optional.of(presence));
        when(roomPresenceRepository.countPresentUsers(any(), any())).thenReturn(3L);

        roomMemberService.leaveRoom("abcdefgh", 1L, CLIENT_ID);

        assertThat(member.getLeftAt()).isNotNull();
        assertThat(room.getEmptySince()).isNull();
    }

    @Test
    void enteringEmptyRoomClearsEmptySince() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        room.markEmpty();
        User user = mock(User.class);
        when(user.getId()).thenReturn(2L);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)).thenReturn(Optional.empty());
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(0L);
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roomMemberService.enterRoom("abcdefgh", 2L, CLIENT_ID);

        assertThat(room.getEmptySince()).isNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);

        ArgumentCaptor<RoomActivityType> typeCaptor = ArgumentCaptor.forClass(RoomActivityType.class);
        verify(roomActivityService).record(any(), typeCaptor.capture(), any(), any(), any());
        assertThat(typeCaptor.getValue()).isEqualTo(RoomActivityType.MEMBER_JOINED);
    }

    @Test
    void leavingClosedRoomDoesNotMarkEmpty() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        RoomMember member = new RoomMember(room, user);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        RoomPresence presence = new RoomPresence(room, user, member, CLIENT_ID,
                java.time.Instant.now().plusSeconds(90));
        when(roomPresenceRepository.findByRoomAndUserAndClientSessionId(room, user, CLIENT_ID))
                .thenReturn(Optional.of(presence));

        roomMemberService.leaveRoom("abcdefgh", 1L, CLIENT_ID);

        assertThat(member.getLeftAt()).isNotNull();
        assertThat(room.getEmptySince()).isNull();
    }

    @Test
    void reconnectRenewsExistingPresenceWithoutDuplicatingMembership() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = user(1L, "Ana", null);
        RoomMember member = new RoomMember(room, user);
        RoomPresence presence = new RoomPresence(room, user, member, CLIENT_ID, Instant.now().plusSeconds(10));
        Instant oldExpiry = presence.getExpiresAt();

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)).thenReturn(Optional.of(member));
        when(roomPresenceRepository.existsByRoomIdAndUserIdAndExpiresAtAfter(eq(1L), eq(1L), any()))
                .thenReturn(true);
        when(roomPresenceRepository.findByRoomAndUserAndClientSessionId(room, user, CLIENT_ID))
                .thenReturn(Optional.of(presence));

        roomMemberService.enterRoom("abcdefgh", 1L, CLIENT_ID);

        assertThat(presence.getExpiresAt()).isAfter(oldExpiry);
        verify(roomMemberRepository, never()).save(any());
        verify(roomActivityService, never()).record(any(), any(), any(), any(), any());
        verifyNoInteractions(roomEventPublisher);
    }

    @Test
    void expiredPresenceRenewalPublishesMembersChanged() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = user(1L, "Ana", null);
        RoomMember member = new RoomMember(room, user);
        RoomPresence presence = new RoomPresence(room, user, member, CLIENT_ID, Instant.now().minusSeconds(1));

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)).thenReturn(Optional.of(member));
        when(roomPresenceRepository.findByRoomAndUserAndClientSessionId(room, user, CLIENT_ID))
                .thenReturn(Optional.of(presence));

        roomMemberService.enterRoom("abcdefgh", 1L, CLIENT_ID);

        verify(roomEventPublisher).publish(eq("ABCDEFGH"), any());
    }

    @Test
    void explicitLeaveKeepsMembershipWhileAnotherTabIsPresent() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = user(1L, "Ana", null);
        RoomMember member = new RoomMember(room, user);
        RoomPresence presence = new RoomPresence(room, user, member, CLIENT_ID, Instant.now().plusSeconds(90));

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomPresenceRepository.findByRoomAndUserAndClientSessionId(room, user, CLIENT_ID))
                .thenReturn(Optional.of(presence));
        when(roomPresenceRepository.existsByRoomIdAndUserIdAndExpiresAtAfter(eq(1L), eq(1L), any()))
                .thenReturn(true);

        roomMemberService.leaveRoom("abcdefgh", 1L, CLIENT_ID);

        assertThat(member.getLeftAt()).isNull();
        verify(roomActivityService, never()).record(any(), any(), any(), any(), any());
        verify(roomPlayerService).releaseIfClaimed(1L, CLIENT_ID);
    }

    @Test
    void staleLegacyMembershipIsReconciled() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = user(1L, "Ana", null);
        RoomMember member = new RoomMember(room, user);
        ReflectionTestUtils.setField(member, "id", 7L);

        when(roomMemberRepository.findOpenMemberIdsWithoutPresence()).thenReturn(List.of(7L));
        when(roomMemberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(roomPresenceRepository.countPresentUsers(eq(room), any())).thenReturn(0L);

        roomMemberService.expireStalePresences();

        assertThat(member.getLeftAt()).isNotNull();
        assertThat(room.getEmptySince()).isNotNull();
        verify(roomPlayerService).releaseIfClaimedByUser(1L, 1L);
    }

    @Test
    void expiredLeaseRemovesPresenceAndStartsRoomGracePeriod() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = user(1L, "Ana", null);
        RoomMember member = new RoomMember(room, user);
        RoomPresence presence = new RoomPresence(room, user, member, CLIENT_ID, Instant.now().minusSeconds(1));
        ReflectionTestUtils.setField(presence, "id", 8L);

        when(roomPresenceRepository.findExpiredCandidates(any()))
                .thenReturn(List.<Object[]>of(new Object[]{8L, 1L}));
        when(roomPresenceRepository.findById(8L)).thenReturn(Optional.of(presence));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(roomPresenceRepository.countPresentUsers(eq(room), any())).thenReturn(0L);

        roomMemberService.expireStalePresences();

        assertThat(member.getLeftAt()).isNotNull();
        assertThat(room.getEmptySince()).isNotNull();
        verify(roomPlayerService).releaseIfClaimed(1L, CLIENT_ID);
    }

    @Test
    void closedRoomCannotBeReentered() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomMemberService.enterRoom("abcdefgh", 1L, CLIENT_ID))
                .isInstanceOf(RoomClosedException.class);

        verifyNoInteractions(roomPresenceRepository);
    }

    @Test
    void staleOpenMembershipCannotBypassRoomCapacity() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", 1L);
        User user = user(1L, "Ana", null);
        RoomMember member = new RoomMember(room, user);

        when(roomRepository.findByCodeForUpdate("ABCDEFGH")).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)).thenReturn(Optional.of(member));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(15L);

        assertThatThrownBy(() -> roomMemberService.enterRoom("abcdefgh", 1L, CLIENT_ID))
                .isInstanceOf(RoomFullException.class);

        verify(roomPresenceRepository, never()).save(any());
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
