package br.com.sintonia.websocket;

public record PlayerChangedEventPayload(String clientSessionId, Long userId) {
}
