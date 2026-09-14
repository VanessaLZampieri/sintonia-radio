package br.com.sintonia.room;

public class NotThePlayerException extends RuntimeException {

    public NotThePlayerException(String message) {
        super(message);
    }
}
