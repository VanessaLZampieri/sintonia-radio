package br.com.sintonia.room;

import java.time.Instant;

public record RoomPlayerResponse(Long roomId, String clientSessionId, Long userId, Instant assumedAt) {

    public static RoomPlayerResponse from(Room room) {
        Long userId = room.getPlayerUser() == null ? null : room.getPlayerUser().getId();
        return new RoomPlayerResponse(
                room.getId(),
                room.getPlayerClientSessionId(),
                userId,
                room.getPlayerAssumedAt());
    }
}
