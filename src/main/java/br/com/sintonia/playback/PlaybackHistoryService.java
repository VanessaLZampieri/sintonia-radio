package br.com.sintonia.playback;

import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlaybackHistoryService {

    private static final List<PlaybackStatus> FINISHED_STATUSES =
            List.of(PlaybackStatus.FINISHED, PlaybackStatus.SKIPPED, PlaybackStatus.ERROR);

    private final RoomRepository roomRepository;
    private final PlaybackRepository playbackRepository;

    public PlaybackHistoryService(RoomRepository roomRepository, PlaybackRepository playbackRepository) {
        this.roomRepository = roomRepository;
        this.playbackRepository = playbackRepository;
    }

    @Transactional(readOnly = true)
    public List<PlaybackHistoryResponse> history(Long roomId, int limit) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return playbackRepository.findByQueueItemRoomIdAndStatusInOrderByStartedAtDesc(
                        roomId, FINISHED_STATUSES, PageRequest.of(0, limit)).stream()
                .map(PlaybackHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> recentSongIds(Long roomId, int limit) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return playbackRepository.findByQueueItemRoomIdAndStatusInOrderByStartedAtDesc(
                        roomId, FINISHED_STATUSES, PageRequest.of(0, limit)).stream()
                .map(playback -> playback.getQueueItem().getSong().getYoutubeVideoId())
                .toList();
    }
}
