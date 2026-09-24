package br.com.sintonia.song;

import java.time.Duration;

public final class SongDurationPolicy {

    public static final Duration MAX_DURATION = Duration.ofMinutes(20);

    private SongDurationPolicy() {
    }

    public static boolean isAllowed(Duration duration) {
        return duration != null && duration.compareTo(MAX_DURATION) <= 0;
    }

    public static void requireAllowed(Duration duration) {
        if (!isAllowed(duration)) {
            throw new SongDurationLimitExceededException(
                    "Esse vídeo é longo demais para a rádio. Escolha uma faixa de até 20 minutos.");
        }
    }
}
