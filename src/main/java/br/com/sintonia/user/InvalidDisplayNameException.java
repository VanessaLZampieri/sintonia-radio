package br.com.sintonia.user;

public class InvalidDisplayNameException extends RuntimeException {

    public InvalidDisplayNameException(String message) {
        super(message);
    }
}
