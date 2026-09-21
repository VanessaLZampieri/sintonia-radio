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

import java.time.Instant;
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
    public RoomStateResponse get(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        Optional<Playback> playing = playbackRepository.findByQueueItemRoomIdAndStatus(roomId, PlaybackStatus.PLAYING);
        Instant now = Instant.now();

        return new RoomStateResponse(
                room.getId(),
                room.getCode(),
                room.getStatus(),
                room.getPlaybackMode(),
                playerState(room),
                playing.map(p -> toPlaybackState(p, now)).orElse(null),
                queueState(roomId),
                playing.map(p -> skipVoteState(room, p, userId)).orElse(null));
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

    private RoomStateResponse.PlaybackState toPlaybackState(Playback playback, Instant now) {
        QueueItem item = playback.getQueueItem();
        return new RoomStateResponse.PlaybackState(
                playback.getId(),
                item.getId(),
                playback.getStartedAt(),
                playback.isPaused(),
                playback.positionSeconds(now),
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

    private RoomStateResponse.SkipVoteState skipVoteState(Room room, Playback playback, Long userId) {
        long participants = roomMemberRepository.countByRoomAndLeftAtIsNull(room);
        long votes = skipVoteRepository.countByPlaybackId(playback.getId());
        boolean currentUserVoted = userId != null
                && skipVoteRepository.existsByPlaybackIdAndUserId(playback.getId(), userId);
        return new RoomStateResponse.SkipVoteState(votes, SkipVoteService.requiredVotes(participants), currentUserVoted);
    }

    private RoomStateResponse.SongState songState(Song song) {
        return new RoomStateResponse.SongState(
                song.getYoutubeVideoId(),
                song.getTitle(),
                song.getThumbnailUrl(),
                song.getDuration().toString());
    }
}
