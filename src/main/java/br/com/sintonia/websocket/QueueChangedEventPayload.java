package br.com.sintonia.websocket;

import java.util.Objects;

public record QueueChangedEventPayload(Long queueItemId, QueueChangeAction action) {

    public QueueChangedEventPayload {
        Objects.requireNonNull(queueItemId, "queueItemId não pode ser nulo");
        Objects.requireNonNull(action, "action não pode ser nulo");
    }
}
