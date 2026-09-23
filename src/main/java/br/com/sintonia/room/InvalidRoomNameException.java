package br.com.sintonia.room;

public class InvalidRoomNameException extends RuntimeException {

    public InvalidRoomNameException(String message) {
        super(message);
    }
}
