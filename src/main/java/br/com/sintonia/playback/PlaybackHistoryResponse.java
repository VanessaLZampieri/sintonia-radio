package br.com.sintonia.playback;

import br.com.sintonia.queue.AddedByResponse;
import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.song.SongResponse;

import java.time.Instant;

public record PlaybackHistoryResponse(
        Long playbackId,
        Long queueItemId,
        SongResponse song,
        Instant startedAt,
        Instant endedAt,
        PlaybackStatus status,
        AddedByResponse addedBy) {

    public static PlaybackHistoryResponse from(Playback playback) {
        QueueItem item = playback.getQueueItem();
        return new PlaybackHistoryResponse(
                playback.getId(),
                item.getId(),
                SongResponse.from(item.getSong()),
                playback.getStartedAt(),
                playback.getEndedAt(),
                playback.getStatus(),
                AddedByResponse.from(item.getUser()));
    }
}
