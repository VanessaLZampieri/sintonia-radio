package br.com.sintonia.room;

import br.com.sintonia.playback.Playback;
import br.com.sintonia.playback.PlaybackRepository;
import br.com.sintonia.playback.PlaybackStatus;
import br.com.sintonia.playback.SkipVoteRepository;
import br.com.sintonia.playback.SkipVoteService;
import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.song.Song;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoomStateService {

    private final RoomRepository roomRepository;
    private final PlaybackRepository playbackRepository;
    private final QueueItemRepository queueItemRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SkipVoteRepository skipVoteRepository;

    public RoomStateService(RoomRepository roomRepository,
                            PlaybackRepository playbackRepository,
                            QueueItemRepository queueItemRepository,
                            RoomMemberRepository roomMemberRepository,
                            SkipVoteRepository skipVoteRepository) {
        this.roomRepository = roomRepository;
        this.playbackRepository = playbackRepository;
        this.queueItemRepository = queueItemRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.skipVoteRepository = skipVoteRepository;
    }

    @Transactional(readOnly = true)
    public RoomStateResponse get(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        Optional<Playback> playing = playbackRepository.findByQueueItemRoomIdAndStatus(roomId, PlaybackStatus.PLAYING);

        return new RoomStateResponse(
                room.getId(),
                room.getCode(),
                room.getStatus(),
                room.getPlaybackMode(),
                playerState(room),
                playing.map(this::toPlaybackState).orElse(null),
                queueState(roomId),
                playing.map(p -> skipVoteState(room, p)).orElse(null));
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

    private RoomStateResponse.PlaybackState toPlaybackState(Playback playback) {
        QueueItem item = playback.getQueueItem();
        return new RoomStateResponse.PlaybackState(
                playback.getId(),
                item.getId(),
                playback.getStartedAt(),
                songState(item.getSong()),
                item.getUser() == null ? null : item.getUser().getId(),
                item.getSource());
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
                item.getUser() == null ? null : item.getUser().getId(),
                item.getSource());
    }

    private RoomStateResponse.SkipVoteState skipVoteState(Room room, Playback playback) {
        long participants = roomMemberRepository.countByRoomAndLeftAtIsNull(room);
        long votes = skipVoteRepository.countByPlaybackId(playback.getId());
        return new RoomStateResponse.SkipVoteState(votes, SkipVoteService.requiredVotes(participants));
    }

    private RoomStateResponse.SongState songState(Song song) {
        return new RoomStateResponse.SongState(
                song.getYoutubeVideoId(),
                song.getTitle(),
                song.getThumbnailUrl(),
                song.getDuration().toString());
    }
}
