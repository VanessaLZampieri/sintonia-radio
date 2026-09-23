package br.com.sintonia.room;

import br.com.sintonia.user.User;

public record RoomParticipantResponse(Long userId, String displayName, String avatarUrl, long waitingCount) {

    public static RoomParticipantResponse from(RoomMember member, long waitingCount) {
        User user = member.getUser();
        return new RoomParticipantResponse(
                user.getId(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                waitingCount);
    }
}
