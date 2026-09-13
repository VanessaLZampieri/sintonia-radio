package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RoomEventPublisher roomEventPublisher;

    @Test
    void publishesToRoomTopic() {
        RoomEvent event = new RoomEvent(RoomEventType.PLAYBACK_STARTED, 1L, Map.of("playbackId", 10L));

        roomEventPublisher.publish("ABC12345", event);

        verify(messagingTemplate).convertAndSend("/topic/rooms/ABC12345", event);
    }

    @Test
    void buildsRoomTopicDestination() {
        assertThat(roomEventPublisher.destinationFor("ABC12345")).isEqualTo("/topic/rooms/ABC12345");
    }

    @Test
    void publishesPayloadWithoutTransformingIt() {
        Map<String, Object> payload = Map.of("queueItemId", 42L);
        RoomEvent event = new RoomEvent(RoomEventType.QUEUE_CHANGED, 1L, payload);

        roomEventPublisher.publish("ABC12345", event);

        verify(messagingTemplate).convertAndSend("/topic/rooms/ABC12345", event);
        assertThat(event.payload()).isSameAs(payload);
    }

    @Test
    void defersPublishUntilAfterCommitWhenTransactionActive() {
        RoomEvent event = new RoomEvent(RoomEventType.PLAYBACK_STARTED, 1L, null);

        TransactionSynchronizationManager.initSynchronization();
        try {
            roomEventPublisher.publish("ABC12345", event);

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(RoomEvent.class));

            TransactionSynchronizationUtils.triggerAfterCommit();

            verify(messagingTemplate).convertAndSend("/topic/rooms/ABC12345", event);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void doesNotPublishWhenTransactionRollsBack() {
        RoomEvent event = new RoomEvent(RoomEventType.PLAYBACK_STARTED, 1L, null);

        TransactionSynchronizationManager.initSynchronization();
        try {
            roomEventPublisher.publish("ABC12345", event);

            TransactionSynchronizationUtils.triggerAfterCompletion(
                    TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(RoomEvent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}