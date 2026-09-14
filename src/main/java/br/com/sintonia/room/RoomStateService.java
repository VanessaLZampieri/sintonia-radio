package br.com.sintonia.room;

import br.com.sintonia.playback.Playback;
import br.com.sintonia.playback.PlaybackRepository;
import br.com.sintonia.playback.PlaybackStatus;
import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.song.Song;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomStateService {

    private final RoomRepository roomRepository;
    private final PlaybackRepository playbackRepository;
    private final QueueItemRepository queueItemRepository;

    public RoomStateService(RoomRepository roomRepository,
                            PlaybackRepository playbackRepository,
                            QueueItemRepository queueItemRepository) {
        this.roomRepository = roomRepository;
        this.playbackRepository = playbackRepository;
        this.queueItemRepository = queueItemRepository;
    }

    @Transactional(readOnly = true)
    public RoomStateResponse get(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return new RoomStateResponse(
                room.getId(),
                room.getCode(),
                room.getStatus(),
                room.getPlaybackMode(),
                playerState(room),
                playbackState(roomId),
                queueState(roomId));
    }

    private RoomStateResponse.PlayerState playerState(Room room) {
        if (room.getPlayerClientSessionId() == null) {
            return null;
        }
        return new RoomStateResponse.PlayerState(
                room.getPlayerClientSessionId(),
                room.getPlayerUser() == null ? null : room.getPlayerUser().getId(),
                room.getPlayerAssumedAt());
    }

    private RoomStateResponse.PlaybackState playbackState(Long roomId) {
        return playbackRepository.findByQueueItemRoomIdAndStatus(roomId, PlaybackStatus.PLAYING)
                .map(this::toPlaybackState)
                .orElse(null);
    }

    private RoomStateResponse.PlaybackState toPlaybackState(Playback playback) {
        QueueItem item = playback.getQueueItem();
        return new RoomStateResponse.PlaybackState(
                playback.getId(),
                item.getId(),
                playback.getStartedAt(),
                songState(item.getSong()),
                item.getUser().getId());
    }

    private List<RoomStateResponse.QueueItemState> queueState(Long roomId) {
        return queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(roomId).stream()
                .map(this::toQueueItemState)
                .toList();
    }

    private RoomStateResponse.QueueItemState toQueueItemState(QueueItem item) {
        return new RoomStateResponse.QueueItemState(
                item.getId(),
                item.getPosition(),
                item.getStatus(),
                songState(item.getSong()),
                item.getUser().getId());
    }

    private RoomStateResponse.SongState songState(Song song) {
        return new RoomStateResponse.SongState(
                song.getYoutubeVideoId(),
                song.getTitle(),
                song.getThumbnailUrl(),
                song.getDuration().toString());
    }
}
