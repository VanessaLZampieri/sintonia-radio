package br.com.sintonia.playback;

public class PlaybackNotPlayingException extends RuntimeException {

    public PlaybackNotPlayingException(String message) {
        super(message);
    }
}