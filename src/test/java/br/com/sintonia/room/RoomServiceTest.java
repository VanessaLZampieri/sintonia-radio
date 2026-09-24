package br.com.sintonia.room;

import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomActivityService roomActivityService;

    @InjectMocks
    private RoomService roomService;

    @Test
    void createRoomTrimsNameAndSavesActiveRoom() {
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room room = roomService.createRoom("  Faxina com sofrimento  ");

        assertThat(room.getName()).isEqualTo("Faxina com sofrimento");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(room.getCode()).hasSize(8);
        assertThat(room.getEmptySince()).isNotNull();
    }

    @Test
    void createRoomRejectsNullName() {
        assertThatThrownBy(() -> roomService.createRoom(null))
                .isInstanceOf(InvalidRoomNameException.class);
    }

    @Test
    void createRoomRejectsTooShortName() {
        assertThatThrownBy(() -> roomService.createRoom("ab"))
                .isInstanceOf(InvalidRoomNameException.class);
    }

    @Test
    void createRoomRejectsTooLongName() {
        assertThatThrownBy(() -> roomService.createRoom("a".repeat(41)))
                .isInstanceOf(InvalidRoomNameException.class);
    }

    @Test
    void renameUpdatesNameWhenMemberAndActive() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        User user = new User("g1", "Ana Souza", "ana@example.com", null);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        RoomResponse response = roomService.rename(ROOM_ID, "  Nova Sala  ", USER_ID);

        assertThat(room.getName()).isEqualTo("Nova Sala");
        assertThat(response.name()).isEqualTo("Nova Sala");
        assertThat(response.code()).isEqualTo("ABCDEFGH");

        ArgumentCaptor<RoomActivityType> typeCaptor = ArgumentCaptor.forClass(RoomActivityType.class);
        verify(roomActivityService).record(any(), typeCaptor.capture(), any(), any(), any());
        assertThat(typeCaptor.getValue()).isEqualTo(RoomActivityType.ROOM_RENAMED);
    }

    @Test
    void renameRejectsUnknownRoom() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.rename(ROOM_ID, "Nova Sala", USER_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void renameRejectsClosedRoom() {
        Room closedRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> roomService.rename(ROOM_ID, "Nova Sala", USER_ID))
                .isInstanceOf(RoomClosedException.class);
    }

    @Test
    void renameRejectsUserNotInRoom() {
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> roomService.rename(ROOM_ID, "Nova Sala", USER_ID))
                .isInstanceOf(UserNotInRoomException.class);
    }

    @Test
    void renameRejectsInvalidName() {
        assertThatThrownBy(() -> roomService.rename(ROOM_ID, "ab", USER_ID))
                .isInstanceOf(InvalidRoomNameException.class);
    }
}
