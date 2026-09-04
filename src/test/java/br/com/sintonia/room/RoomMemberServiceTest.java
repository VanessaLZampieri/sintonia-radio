package br.com.sintonia.room;

import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomMemberServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoomMemberService roomMemberService;

    @Test
    void lastMemberLeavingMarksRoomAsEmpty() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
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
    }

    @Test
    void leavingWithOtherMembersDoesNotMarkRoomAsEmpty() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
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
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
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
    }

    @Test
    void leavingClosedRoomDoesNotMarkEmpty() {
        Room room = new Room("ABCDEFGH", RoomStatus.CLOSED);
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
}
