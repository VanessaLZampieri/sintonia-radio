package br.com.sintonia.room;

import java.time.Instant;

public record RoomActivityResponse(
        Long id,
        RoomActivityType type,
        Long actorUserId,
        String actorDisplayName,
        Long songId,
        String songTitle,
        String detail,
        Instant createdAt) {

    public static RoomActivityResponse from(RoomActivity activity) {
        return new RoomActivityResponse(
                activity.getId(),
                activity.getType(),
                activity.getActorUserId(),
                activity.getActorDisplayName(),
                activity.getSongId(),
                activity.getSongTitle(),
                activity.getDetail(),
                activity.getCreatedAt());
    }
}
