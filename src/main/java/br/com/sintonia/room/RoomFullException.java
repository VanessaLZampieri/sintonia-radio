package br.com.sintonia.room;

public class RoomFullException extends RuntimeException {

    public RoomFullException(String message) {
        super(message);
    }
}
