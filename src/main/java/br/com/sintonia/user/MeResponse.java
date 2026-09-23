package br.com.sintonia.user;

public record MeResponse(Long id, String name, String displayName, String email, String avatarUrl) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getName(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarUrl());
    }
}
