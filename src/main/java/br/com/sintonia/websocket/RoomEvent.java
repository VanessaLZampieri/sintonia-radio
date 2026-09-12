package br.com.sintonia.websocket;

import java.util.Objects;

public record RoomEvent(RoomEventType eventType, Long roomId, Object payload) {

    public RoomEvent {
        Objects.requireNonNull(eventType, "eventType não pode ser nulo");
        Objects.requireNonNull(roomId, "roomId não pode ser nulo");
    }
}