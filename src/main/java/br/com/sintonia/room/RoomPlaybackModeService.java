package br.com.sintonia.room;

import br.com.sintonia.playback.Playback;
import br.com.sintonia.playback.PlaybackRepository;
import br.com.sintonia.playback.PlaybackStatus;
import br.com.sintonia.websocket.PlaybackEventPayload;
import br.com.sintonia.websocket.PlaybackModeChangedEventPayload;
import br.com.sintonia.websocket.PlayerChangedEventPayload;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import br.com.sintonia.websocket.RoomEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomPlaybackModeService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final PlaybackRepository playbackRepository;
    private final RoomEventPublisher roomEventPublisher;

    public RoomPlaybackModeService(RoomRepository roomRepository,
                                   RoomMemberRepository roomMemberRepository,
                                   PlaybackRepository playbackRepository,
                                   RoomEventPublisher roomEventPublisher) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.playbackRepository = playbackRepository;
        this.roomEventPublisher = roomEventPublisher;
    }

    @Transactional(readOnly = true)
    public PlaybackModeResponse current(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return new PlaybackModeResponse(room.getId(), room.getPlaybackMode());
    }

    @Transactional
    public PlaybackModeResponse change(Long roomId, PlaybackMode mode, Long userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        if (!roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)) {
            throw new UserNotInRoomException("Usuário não está na sala.");
        }

        if (room.getPlaybackMode() == mode) {
            return new PlaybackModeResponse(room.getId(), room.getPlaybackMode());
        }

        room.changePlaybackMode(mode);

        boolean releasedPlayer = false;
        if (mode == PlaybackMode.TODOS_OS_NAVEGADORES && room.getPlayerClientSessionId() != null) {
            room.release();
            releasedPlayer = true;
        }

        if (releasedPlayer) {
            roomEventPublisher.publish(room.getCode(),
                    new RoomEvent(RoomEventType.PLAYER_CHANGED, room.getId(),
                            new PlayerChangedEventPayload(null, null)));
        }

        if (mode == PlaybackMode.TODOS_OS_NAVEGADORES) {
            resumePausedPlayback(room);
        }

        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.PLAYBACK_MODE_CHANGED, room.getId(),
                        new PlaybackModeChangedEventPayload(mode)));

        return new PlaybackModeResponse(room.getId(), room.getPlaybackMode());
    }

    private void resumePausedPlayback(Room room) {
        playbackRepository.findByQueueItemRoomIdAndStatus(room.getId(), PlaybackStatus.PLAYING)
                .ifPresent(playback -> {
                    if (playback.isPaused()) {
                        playback.resume();
                        playbackRepository.save(playback);
                        roomEventPublisher.publish(room.getCode(),
                                new RoomEvent(RoomEventType.PLAYBACK_RESUMED, room.getId(),
                                        new PlaybackEventPayload(playback.getId(),
                                                playback.getQueueItem().getId())));
                    }
                });
    }
}
