package br.com.sintonia.websocket;

import br.com.sintonia.room.RoomPlayerService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class PlayerDisconnectListener {

    private final ClientSessionRegistry clientSessionRegistry;
    private final RoomPlayerService roomPlayerService;

    public PlayerDisconnectListener(ClientSessionRegistry clientSessionRegistry, RoomPlayerService roomPlayerService) {
        this.clientSessionRegistry = clientSessionRegistry;
        this.roomPlayerService = roomPlayerService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        clientSessionRegistry.unregister(event.getSessionId());
    }

    @EventListener
    public void onClientSessionLost(ClientSessionLostEvent event) {
        roomPlayerService.releaseByClientSessionId(event.clientSessionId());
    }
}
