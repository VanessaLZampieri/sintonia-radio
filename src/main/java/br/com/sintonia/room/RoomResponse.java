package br.com.sintonia.room;

import java.time.Instant;

public record RoomResponse(Long id, String code, String name, RoomStatus status, Instant createdAt) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getCode(), room.getName(), room.getStatus(), room.getCreatedAt());
    }
}
