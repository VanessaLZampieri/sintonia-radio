package br.com.sintonia.websocket;

import br.com.sintonia.room.RoomPlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerDisconnectListenerTest {

    @Test
    void disconnectUnregistersSession() {
        ClientSessionRegistry registry = mock(ClientSessionRegistry.class);
        RoomPlayerService roomPlayerService = mock(RoomPlayerService.class);
        PlayerDisconnectListener listener = new PlayerDisconnectListener(registry, roomPlayerService);
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn("session-1");

        listener.onDisconnect(event);

        verify(registry).unregister("session-1");
    }

    @Test
    void clientSessionLostReleasesPlayer() {
        ClientSessionRegistry registry = mock(ClientSessionRegistry.class);
        RoomPlayerService roomPlayerService = mock(RoomPlayerService.class);
        PlayerDisconnectListener listener = new PlayerDisconnectListener(registry, roomPlayerService);

        listener.onClientSessionLost(new ClientSessionLostEvent("550e8400-e29b-41d4-a716-446655440000"));

        verify(roomPlayerService).releaseByClientSessionId("550e8400-e29b-41d4-a716-446655440000");
    }
}
