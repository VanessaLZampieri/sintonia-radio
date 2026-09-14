package br.com.sintonia.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ClientSessionInterceptorTest {

    private static final String UUID = "550e8400-e29b-41d4-a716-446655440000";

    private ClientSessionRegistry registry;
    private ClientSessionInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        registry = mock(ClientSessionRegistry.class);
        interceptor = new ClientSessionInterceptor(registry);
        channel = mock(MessageChannel.class);
    }

    @Test
    void connectWithValidHeaderRegisters() {
        Message<byte[]> message = connectMessage("session-1", UUID);

        interceptor.preSend(message, channel);

        verify(registry).register("session-1", UUID);
    }

    @Test
    void connectWithoutHeaderIsRejected() {
        Message<byte[]> message = connectMessage("session-1", null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);

        verify(registry, never()).register(any(), any());
    }

    @Test
    void connectWithBlankHeaderIsRejected() {
        Message<byte[]> message = connectMessage("session-1", "   ");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);

        verify(registry, never()).register(any(), any());
    }

    @Test
    void connectWithInvalidUuidIsRejected() {
        Message<byte[]> message = connectMessage("session-1", "abc");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);

        verify(registry, never()).register(any(), any());
    }

    @Test
    void nonConnectCommandDoesNotRegister() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setNativeHeader("clientSessionId", UUID);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, channel);

        verify(registry, never()).register(any(), any());
    }

    private Message<byte[]> connectMessage(String sessionId, String clientSessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId(sessionId);
        if (clientSessionId != null) {
            accessor.setNativeHeader("clientSessionId", clientSessionId);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
