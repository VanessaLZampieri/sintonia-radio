package br.com.sintonia.room;

public class UserNotInRoomException extends RuntimeException {

    public UserNotInRoomException(String message) {
        super(message);
    }
}
