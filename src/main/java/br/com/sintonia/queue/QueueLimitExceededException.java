package br.com.sintonia.queue;

public class QueueLimitExceededException extends RuntimeException {

    public QueueLimitExceededException(String message) {
        super(message);
    }
}
