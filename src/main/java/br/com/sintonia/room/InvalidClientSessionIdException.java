package br.com.sintonia.room;

public class InvalidClientSessionIdException extends RuntimeException {

    public InvalidClientSessionIdException(String message) {
        super(message);
    }
}
