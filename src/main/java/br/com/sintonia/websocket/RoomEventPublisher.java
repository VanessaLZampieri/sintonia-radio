package br.com.sintonia.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RoomEventPublisher {

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

    private final SimpMessagingTemplate messagingTemplate;

    public RoomEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(String roomCode, RoomEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(destinationFor(roomCode), event);
                }
            });
        } else {
            messagingTemplate.convertAndSend(destinationFor(roomCode), event);
        }
    }

    public String destinationFor(String roomCode) {
        return ROOM_TOPIC_PREFIX + roomCode;
    }
}