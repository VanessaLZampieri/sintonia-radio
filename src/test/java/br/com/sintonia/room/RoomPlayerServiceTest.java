package br.com.sintonia.room;

import br.com.sintonia.user.User;
import br.com.sintonia.user.UserNotFoundException;
import br.com.sintonia.user.UserRepository;
import br.com.sintonia.websocket.PlayerChangedEventPayload;
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

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomPlayerServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String OTHER_SESSION_ID = "550e8400-e29b-41d4-a716-446655440001";

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    @InjectMocks
    private RoomPlayerService roomPlayerService;

    private Room room;
    private User user;

    @BeforeEach
    void setUp() {
        room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        room.changePlaybackMode(PlaybackMode.CAIXA_DE_MUSICA);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        user = newUser(USER_ID);
    }

    @Test
    void claimsPlayerWhenRoomIsFree() {
        stubClaimHappyPath();

        RoomPlayerResponse result = roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID);

        assertThat(result.clientSessionId()).isEqualTo(SESSION_ID);
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.assumedAt()).isNotNull();
        assertThat(room.getPlayerClientSessionId()).isEqualTo(SESSION_ID);
        assertThat(room.getPlayerUser()).isSameAs(user);
        assertThat(room.getPlayerAssumedAt()).isNotNull();
    }

    @Test
    void sameSessionClaimIsIdempotent() {
        makePlayer(SESSION_ID);
        stubClaimHappyPath();

        RoomPlayerResponse result = roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID);

        assertThat(result.clientSessionId()).isEqualTo(SESSION_ID);
        verifyNoInteractions(roomEventPublisher);
    }

    @Test
    void secondSessionClaimIsRejected() {
        makePlayer(OTHER_SESSION_ID);
        stubClaimHappyPath();

        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .isInstanceOf(PlayerAlreadyClaimedException.class);
    }

    @Test
    void claimRejectsUnknownUser() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void claimRejectsUserNotInRoom() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .isInstanceOf(UserNotInRoomException.class);
    }

    @Test
    void claimRejectsUnknownRoom() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void claimRejectsClosedRoom() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .isInstanceOf(RoomClosedException.class);
    }

    @Test
    void claimRejectsInvalidSessionId() {
        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, null, USER_ID))
                .isInstanceOf(InvalidClientSessionIdException.class);
        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, "   ", USER_ID))
                .isInstanceOf(InvalidClientSessionIdException.class);
        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, "abc", USER_ID))
                .isInstanceOf(InvalidClientSessionIdException.class);
    }

    @Test
    void claimRejectedInTodosOsNavegadoresMode() {
        Room todosRoom = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(todosRoom));

        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .isInstanceOf(ClaimNotAllowedException.class);

        verifyNoInteractions(roomEventPublisher);
    }

    @Test
    void releaseByClientSessionIdReleasesPlayer() {
        makePlayer(SESSION_ID);
        when(roomRepository.findByPlayerClientSessionId(SESSION_ID)).thenReturn(List.of(room));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        roomPlayerService.releaseByClientSessionId(SESSION_ID);

        assertThat(room.getPlayerClientSessionId()).isNull();
        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RoomEventType.PLAYER_CHANGED);
    }

    @Test
    void releaseByClientSessionIdNoOpWhenNotPlayer() {
        when(roomRepository.findByPlayerClientSessionId(SESSION_ID)).thenReturn(List.of());

        roomPlayerService.releaseByClientSessionId(SESSION_ID);

        verifyNoInteractions(roomEventPublisher);
    }

    @Test
    void releaseRejectsInvalidSessionId() {
        assertThatThrownBy(() -> roomPlayerService.release(ROOM_ID, "abc"))
                .isInstanceOf(InvalidClientSessionIdException.class);
    }

    @Test
    void releaseByPlayerSucceeds() {
        makePlayer(SESSION_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        RoomPlayerResponse result = roomPlayerService.release(ROOM_ID, SESSION_ID);

        assertThat(result.clientSessionId()).isNull();
        assertThat(result.userId()).isNull();
        assertThat(room.getPlayerClientSessionId()).isNull();
        assertThat(room.getPlayerUser()).isNull();
        assertThat(room.getPlayerAssumedAt()).isNull();
    }

    @Test
    void releaseByOtherSessionIsRejected() {
        makePlayer(OTHER_SESSION_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomPlayerService.release(ROOM_ID, SESSION_ID))
                .isInstanceOf(NotThePlayerException.class);
    }

    @Test
    void releaseWhenNoPlayerIsIdempotent() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        RoomPlayerResponse result = roomPlayerService.release(ROOM_ID, SESSION_ID);

        assertThat(result.clientSessionId()).isNull();
        verifyNoInteractions(roomEventPublisher);
    }

    @Test
    void idempotentClaimDoesNotChangePlayerAssumedAt() {
        makePlayer(SESSION_ID);
        Instant before = room.getPlayerAssumedAt();
        stubClaimHappyPath();

        roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID);

        assertThat(room.getPlayerAssumedAt()).isEqualTo(before);
    }

    @Test
    void claimPublishesPlayerChangedEvent() {
        stubClaimHappyPath();

        roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        RoomEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RoomEventType.PLAYER_CHANGED);
        assertThat(event.roomId()).isEqualTo(ROOM_ID);
        PlayerChangedEventPayload payload = (PlayerChangedEventPayload) event.payload();
        assertThat(payload.clientSessionId()).isEqualTo(SESSION_ID);
        assertThat(payload.userId()).isEqualTo(USER_ID);
    }

    @Test
    void releasePublishesPlayerChangedEvent() {
        makePlayer(SESSION_ID);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        roomPlayerService.release(ROOM_ID, SESSION_ID);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        RoomEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RoomEventType.PLAYER_CHANGED);
        assertThat(event.roomId()).isEqualTo(ROOM_ID);
        PlayerChangedEventPayload payload = (PlayerChangedEventPayload) event.payload();
        assertThat(payload.clientSessionId()).isNull();
        assertThat(payload.userId()).isNull();
    }

    @Test
    void rejectedClaimDoesNotPublishEvent() {
        makePlayer(OTHER_SESSION_ID);
        stubClaimHappyPath();

        assertThatThrownBy(() -> roomPlayerService.claim(ROOM_ID, SESSION_ID, USER_ID))
                .isInstanceOf(PlayerAlreadyClaimedException.class);

        verify(roomEventPublisher, never()).publish(eq("ABCDEFGH"), org.mockito.ArgumentMatchers.any(RoomEvent.class));
    }

    @Test
    void currentReturnsCurrentPlayer() {
        makePlayer(SESSION_ID);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

        RoomPlayerResponse result = roomPlayerService.current(ROOM_ID);

        assertThat(result.clientSessionId()).isEqualTo(SESSION_ID);
        assertThat(result.userId()).isEqualTo(USER_ID);
    }

    @Test
    void currentReturnsEmptyWhenNoPlayer() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

        RoomPlayerResponse result = roomPlayerService.current(ROOM_ID);

        assertThat(result.clientSessionId()).isNull();
        assertThat(result.userId()).isNull();
        assertThat(result.assumedAt()).isNull();
    }

    private void stubClaimHappyPath() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private void makePlayer(String sessionId) {
        room.claim(sessionId, user);
    }

    private User newUser(Long id) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            ReflectionTestUtils.setField(user, "id", id);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível criar User de teste", e);
        }
    }
}
