package br.com.sintonia.room;

public class ClaimNotAllowedException extends RuntimeException {

    public ClaimNotAllowedException(String message) {
        super(message);
    }
}
