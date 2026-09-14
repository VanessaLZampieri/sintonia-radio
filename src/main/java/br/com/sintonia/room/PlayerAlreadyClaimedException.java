package br.com.sintonia.room;

public class PlayerAlreadyClaimedException extends RuntimeException {

    public PlayerAlreadyClaimedException(String message) {
        super(message);
    }
}
