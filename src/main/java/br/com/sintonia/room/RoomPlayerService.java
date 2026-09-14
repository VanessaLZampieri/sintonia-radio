package br.com.sintonia.room;

import br.com.sintonia.user.User;
import br.com.sintonia.user.UserNotFoundException;
import br.com.sintonia.user.UserRepository;
import br.com.sintonia.websocket.PlayerChangedEventPayload;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import br.com.sintonia.websocket.RoomEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RoomPlayerService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher roomEventPublisher;

    public RoomPlayerService(RoomRepository roomRepository,
                             RoomMemberRepository roomMemberRepository,
                             UserRepository userRepository,
                             RoomEventPublisher roomEventPublisher) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.roomEventPublisher = roomEventPublisher;
    }

    @Transactional(readOnly = true)
    public RoomPlayerResponse current(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return RoomPlayerResponse.from(room);
    }

    @Transactional
    public RoomPlayerResponse claim(Long roomId, String clientSessionId, Long userId) {
        validateClientSessionId(clientSessionId);

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        if (room.getPlaybackMode() != PlaybackMode.CAIXA_DE_MUSICA) {
            throw new ClaimNotAllowedException("Claim de player só é permitido no modo CAIXA_DE_MUSICA.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        if (!roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)) {
            throw new UserNotInRoomException("Usuário não está na sala.");
        }

        String currentSession = room.getPlayerClientSessionId();
        if (currentSession != null) {
            if (currentSession.equals(clientSessionId)) {
                return RoomPlayerResponse.from(room);
            }
            throw new PlayerAlreadyClaimedException("Já existe um player nesta sala.");
        }

        room.claim(clientSessionId, user);
        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.PLAYER_CHANGED, roomId,
                        new PlayerChangedEventPayload(clientSessionId, userId)));
        return RoomPlayerResponse.from(room);
    }

    @Transactional
    public RoomPlayerResponse release(Long roomId, String clientSessionId) {
        validateClientSessionId(clientSessionId);

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        String currentSession = room.getPlayerClientSessionId();
        if (currentSession == null) {
            return RoomPlayerResponse.from(room);
        }
        if (!currentSession.equals(clientSessionId)) {
            throw new NotThePlayerException("A sessão informada não é o player atual.");
        }

        room.release();
        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.PLAYER_CHANGED, roomId,
                        new PlayerChangedEventPayload(null, null)));
        return RoomPlayerResponse.from(room);
    }

    @Transactional
    public void releaseByClientSessionId(String clientSessionId) {
        for (Room room : roomRepository.findByPlayerClientSessionId(clientSessionId)) {
            Room locked = roomRepository.findByIdForUpdate(room.getId()).orElse(null);
            if (locked == null) {
                continue;
            }
            if (clientSessionId.equals(locked.getPlayerClientSessionId())) {
                locked.release();
                roomEventPublisher.publish(locked.getCode(),
                        new RoomEvent(RoomEventType.PLAYER_CHANGED, locked.getId(),
                                new PlayerChangedEventPayload(null, null)));
            }
        }
    }

    private void validateClientSessionId(String clientSessionId) {
        if (clientSessionId == null || clientSessionId.isBlank()) {
            throw new InvalidClientSessionIdException("clientSessionId é obrigatório.");
        }
        try {
            UUID.fromString(clientSessionId);
        } catch (IllegalArgumentException exception) {
            throw new InvalidClientSessionIdException("clientSessionId deve ser um UUID válido.");
        }
    }
}
