package br.com.sintonia.playback;

public class PlaybackAlreadyInProgressException extends RuntimeException {

    public PlaybackAlreadyInProgressException(String message) {
        super(message);
    }
}