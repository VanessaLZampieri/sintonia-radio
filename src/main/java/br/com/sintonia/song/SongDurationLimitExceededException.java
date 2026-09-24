package br.com.sintonia.song;

public class SongDurationLimitExceededException extends RuntimeException {

    public SongDurationLimitExceededException(String message) {
        super(message);
    }
}
