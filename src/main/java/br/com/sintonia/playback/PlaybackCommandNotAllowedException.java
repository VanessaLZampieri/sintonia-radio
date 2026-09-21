package br.com.sintonia.playback;

public class PlaybackCommandNotAllowedException extends RuntimeException {

    public PlaybackCommandNotAllowedException(String message) {
        super(message);
    }
}
