package br.com.sintonia.websocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ClientSessionRegistryTest {

    private static final String CLIENT_ID = "550e8400-e29b-41d4-a716-446655440000";

    private ApplicationEventPublisher eventPublisher;
    private ClientSessionRegistry registry;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        registry = new ClientSessionRegistry(eventPublisher, Duration.ofMillis(50));
    }

    @AfterEach
    void tearDown() {
        registry.shutdown();
    }

    @Test
    void lastConnectionDisconnectPublishesClientSessionLost() {
        registry.register("session-1", CLIENT_ID);

        registry.unregister("session-1");

        sleep(200);
        verify(eventPublisher).publishEvent(new ClientSessionLostEvent(CLIENT_ID));
    }

    @Test
    void multipleConnectionsDoNotPublishUntilLast() {
        registry.register("session-1", CLIENT_ID);
        registry.register("session-2", CLIENT_ID);

        registry.unregister("session-1");

        sleep(200);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void reconnectCancelsPendingRelease() {
        registry.register("session-1", CLIENT_ID);
        registry.unregister("session-1");

        registry.register("session-2", CLIENT_ID);

        sleep(200);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void duplicatedDisconnectIsIdempotent() {
        registry.register("session-1", CLIENT_ID);
        registry.unregister("session-1");
        registry.unregister("session-1");

        sleep(200);
        verify(eventPublisher, times(1)).publishEvent(new ClientSessionLostEvent(CLIENT_ID));
    }

    @Test
    void disconnectWithoutRegistrationDoesNothing() {
        registry.unregister("unknown-session");

        sleep(200);
        verify(eventPublisher, never()).publishEvent(any());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
