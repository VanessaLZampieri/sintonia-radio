package br.com.sintonia.room;

import br.com.sintonia.queue.QueueItemSource;
import br.com.sintonia.queue.QueueItemStatus;

import java.time.Instant;
import java.util.List;

public record RoomStateResponse(
        Long roomId,
        String roomCode,
        RoomStatus status,
        PlaybackMode playbackMode,
        PlayerState player,
        PlaybackState currentPlayback,
        List<QueueItemState> queue,
        SkipVoteState skipVote) {

    public record PlayerState(String clientSessionId, Long userId, Instant assumedAt) {
    }

    public record SongState(String youtubeVideoId, String title, String thumbnailUrl, String duration) {
    }

    public record PlaybackState(Long playbackId, Long queueItemId, Instant startedAt, boolean paused,
                                long positionSeconds, SongState song, Long addedByUserId,
                                QueueItemSource source) {
    }

    public record QueueItemState(Long queueItemId, Integer position, QueueItemStatus status, SongState song,
                                 Long addedByUserId, QueueItemSource source) {
    }

    public record SkipVoteState(long votes, long requiredVotes, boolean currentUserVoted) {
    }
}
