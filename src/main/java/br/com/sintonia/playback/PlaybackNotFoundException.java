package br.com.sintonia.playback;

public class PlaybackNotFoundException extends RuntimeException {

    public PlaybackNotFoundException(String message) {
        super(message);
    }
}