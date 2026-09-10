package br.com.sintonia.queue;

public class QueueItemNotFoundException extends RuntimeException {

    public QueueItemNotFoundException(String message) {
        super(message);
    }
}