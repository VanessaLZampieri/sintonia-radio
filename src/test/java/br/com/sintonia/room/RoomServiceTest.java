package br.com.sintonia.room;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;

    @Test
    void createRoomTrimsNameAndSavesActiveRoom() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        Room room = service.createRoom("  Faxina com sofrimento  ");

        assertThat(room.getName()).isEqualTo("Faxina com sofrimento");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(room.getCode()).hasSize(8);
    }

    @Test
    void createRoomRejectsNullName() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        assertThatThrownBy(() -> service.createRoom(null))
                .isInstanceOf(InvalidRoomNameException.class);
    }

    @Test
    void createRoomRejectsTooShortName() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        assertThatThrownBy(() -> service.createRoom("ab"))
                .isInstanceOf(InvalidRoomNameException.class);
    }

    @Test
    void createRoomRejectsTooLongName() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        assertThatThrownBy(() -> service.createRoom("a".repeat(41)))
                .isInstanceOf(InvalidRoomNameException.class);
    }

    @Test
    void renameUpdatesNameWhenMemberAndActive() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        RoomResponse response = service.rename(ROOM_ID, "  Nova Sala  ", USER_ID);

        assertThat(room.getName()).isEqualTo("Nova Sala");
        assertThat(response.name()).isEqualTo("Nova Sala");
        assertThat(response.code()).isEqualTo("ABCDEFGH");
    }

    @Test
    void renameRejectsUnknownRoom() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        assertThatThrownBy(() -> service.rename(ROOM_ID, "Nova Sala", USER_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void renameRejectsClosedRoom() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);
        Room closedRoom = new Room("Sala Teste", "ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        assertThatThrownBy(() -> service.rename(ROOM_ID, "Nova Sala", USER_ID))
                .isInstanceOf(RoomClosedException.class);
    }

    @Test
    void renameRejectsUserNotInRoom() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);
        Room room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        assertThatThrownBy(() -> service.rename(ROOM_ID, "Nova Sala", USER_ID))
                .isInstanceOf(UserNotInRoomException.class);
    }

    @Test
    void renameRejectsInvalidName() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomMemberRepository roomMemberRepository = mock(RoomMemberRepository.class);

        RoomService service = new RoomService(roomRepository, roomMemberRepository);

        assertThatThrownBy(() -> service.rename(ROOM_ID, "ab", USER_ID))
                .isInstanceOf(InvalidRoomNameException.class);
    }
}
