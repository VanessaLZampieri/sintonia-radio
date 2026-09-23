package br.com.sintonia.room;

import br.com.sintonia.user.User;

public record RoomParticipantResponse(Long userId, String displayName, String avatarUrl) {

    public static RoomParticipantResponse from(RoomMember member) {
        User user = member.getUser();
        return new RoomParticipantResponse(
                user.getId(),
                user.getDisplayName(),
                user.getAvatarUrl());
    }
}
