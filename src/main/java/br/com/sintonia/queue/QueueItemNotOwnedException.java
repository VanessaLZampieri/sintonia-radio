package br.com.sintonia.queue;

public class QueueItemNotOwnedException extends RuntimeException {

    public QueueItemNotOwnedException(String message) {
        super(message);
    }
}
