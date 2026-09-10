package br.com.sintonia.queue;

public class SongAlreadyInQueueException extends RuntimeException {

    public SongAlreadyInQueueException(String message) {
        super(message);
    }
}
