package br.com.sintonia.queue;

import br.com.sintonia.user.User;

public record AddedByResponse(Long id, String name, String avatarUrl) {

    public static AddedByResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new AddedByResponse(
                user.getId(),
                user.getName(),
                user.getAvatarUrl());
    }
}