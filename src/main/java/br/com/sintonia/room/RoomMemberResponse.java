package br.com.sintonia.room;

import java.time.Instant;

public record RoomMemberResponse(Long id, Long roomId, String roomCode, Long userId, Instant joinedAt, Instant leftAt) {

    public static RoomMemberResponse from(RoomMember member) {
        return new RoomMemberResponse(
                member.getId(),
                member.getRoom().getId(),
                member.getRoom().getCode(),
                member.getUser().getId(),
                member.getJoinedAt(),
                member.getLeftAt());
    }
}
