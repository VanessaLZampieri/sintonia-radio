package br.com.sintonia.websocket;

import br.com.sintonia.room.PlaybackMode;

import java.util.Objects;

public record PlaybackModeChangedEventPayload(PlaybackMode mode) {

    public PlaybackModeChangedEventPayload {
        Objects.requireNonNull(mode, "mode não pode ser nulo");
    }
}
