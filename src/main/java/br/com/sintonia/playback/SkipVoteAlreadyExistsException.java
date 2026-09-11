package br.com.sintonia.playback;

public class SkipVoteAlreadyExistsException extends RuntimeException {

    public SkipVoteAlreadyExistsException(String message) {
        super(message);
    }
}