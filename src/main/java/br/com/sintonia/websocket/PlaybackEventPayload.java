package br.com.sintonia.websocket;

import java.util.Objects;

public record PlaybackEventPayload(Long playbackId, Long queueItemId) {

    public PlaybackEventPayload {
        Objects.requireNonNull(playbackId, "playbackId não pode ser nulo");
        Objects.requireNonNull(queueItemId, "queueItemId não pode ser nulo");
    }
}