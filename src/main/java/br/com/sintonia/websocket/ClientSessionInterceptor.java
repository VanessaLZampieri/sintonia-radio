package br.com.sintonia.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClientSessionInterceptor implements ChannelInterceptor {

    private static final String CLIENT_SESSION_HEADER = "clientSessionId";

    private final ClientSessionRegistry clientSessionRegistry;

    public ClientSessionInterceptor(ClientSessionRegistry clientSessionRegistry) {
        this.clientSessionRegistry = clientSessionRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            String clientSessionId = accessor.getFirstNativeHeader(CLIENT_SESSION_HEADER);
            if (sessionId == null || !isValidUuid(clientSessionId)) {
                throw new MessageDeliveryException("clientSessionId inválido ou ausente no CONNECT.");
            }
            clientSessionRegistry.register(sessionId, clientSessionId);
        }
        return message;
    }

    private boolean isValidUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
