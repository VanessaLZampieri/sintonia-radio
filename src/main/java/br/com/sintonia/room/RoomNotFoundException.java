package br.com.sintonia.room;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String message) {
        super(message);
    }
}
