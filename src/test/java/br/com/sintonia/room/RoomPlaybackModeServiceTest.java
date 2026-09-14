package br.com.sintonia.room;

import br.com.sintonia.user.User;
import br.com.sintonia.websocket.PlaybackModeChangedEventPayload;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomPlaybackModeServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    @InjectMocks
    private RoomPlaybackModeService roomPlaybackModeService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
    }

    @Test
    void currentReturnsDefaultMode() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

        PlaybackModeResponse result = roomPlaybackModeService.current(ROOM_ID);

        assertThat(result.mode()).isEqualTo(PlaybackMode.TODOS_OS_NAVEGADORES);
    }

    @Test
    void changeUpdatesModeAndPublishesEvent() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        PlaybackModeResponse result = roomPlaybackModeService.change(ROOM_ID, PlaybackMode.CAIXA_DE_MUSICA);

        assertThat(result.mode()).isEqualTo(PlaybackMode.CAIXA_DE_MUSICA);
        assertThat(room.getPlaybackMode()).isEqualTo(PlaybackMode.CAIXA_DE_MUSICA);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RoomEventType.PLAYBACK_MODE_CHANGED);
        PlaybackModeChangedEventPayload payload = (PlaybackModeChangedEventPayload) captor.getValue().payload();
        assertThat(payload.mode()).isEqualTo(PlaybackMode.CAIXA_DE_MUSICA);
    }

    @Test
    void changeToSameModeIsIdempotent() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        PlaybackModeResponse result = roomPlaybackModeService.change(ROOM_ID, PlaybackMode.TODOS_OS_NAVEGADORES);

        assertThat(result.mode()).isEqualTo(PlaybackMode.TODOS_OS_NAVEGADORES);
        verifyNoInteractions(roomEventPublisher);
    }

    @Test
    void changeToTodosOsNavegadoresClearsPlayer() {
        room.changePlaybackMode(PlaybackMode.CAIXA_DE_MUSICA);
        room.claim(SESSION_ID, mock(User.class));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        roomPlaybackModeService.change(ROOM_ID, PlaybackMode.TODOS_OS_NAVEGADORES);

        assertThat(room.getPlayerClientSessionId()).isNull();
        assertThat(room.getPlayerUser()).isNull();
        assertThat(room.getPlayerAssumedAt()).isNull();

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher, times(2)).publish(eq("ABCDEFGH"), captor.capture());
        var events = captor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).eventType()).isEqualTo(RoomEventType.PLAYER_CHANGED);
        assertThat(events.get(1).eventType()).isEqualTo(RoomEventType.PLAYBACK_MODE_CHANGED);
    }

    @Test
    void changeToCaixaDeMusicaDoesNotCreatePlayer() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        roomPlaybackModeService.change(ROOM_ID, PlaybackMode.CAIXA_DE_MUSICA);

        assertThat(room.getPlayerClientSessionId()).isNull();
        assertThat(room.getPlayerUser()).isNull();
    }

    @Test
    void changeRejectsUnknownRoom() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomPlaybackModeService.change(ROOM_ID, PlaybackMode.CAIXA_DE_MUSICA))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void changeRejectsClosedRoom() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> roomPlaybackModeService.change(ROOM_ID, PlaybackMode.CAIXA_DE_MUSICA))
                .isInstanceOf(RoomClosedException.class);
    }
}
