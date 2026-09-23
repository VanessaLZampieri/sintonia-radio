package br.com.sintonia.room;

public record UserRoomResponse(
        Long roomId,
        String name,
        String code,
        RoomStatus status,
        boolean canEnter,
        long participantCount) {
}
