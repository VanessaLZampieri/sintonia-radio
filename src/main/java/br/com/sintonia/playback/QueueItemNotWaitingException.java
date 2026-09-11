package br.com.sintonia.playback;

public class QueueItemNotWaitingException extends RuntimeException {

    public QueueItemNotWaitingException(String message) {
        super(message);
    }
}