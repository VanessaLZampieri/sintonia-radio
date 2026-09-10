package br.com.sintonia.queue;

import br.com.sintonia.song.SongResponse;
import java.time.Instant;

public record QueueItemResponse(Long id, Integer position, Instant addedAt,
                                  SongResponse song, AddedByResponse addedBy) {

    public static QueueItemResponse from(QueueItem item) {
        return new QueueItemResponse(
                item.getId(),
                item.getPosition(),
                item.getAddedAt(),
                SongResponse.from(item.getSong()),
                AddedByResponse.from(item.getUser()));
    }
}