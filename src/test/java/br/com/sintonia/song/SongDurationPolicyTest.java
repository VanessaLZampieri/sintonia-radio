package br.com.sintonia.song;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SongDurationPolicyTest {

    @Test
    void allowsDurationBelowTwentyMinutes() {
        assertThat(SongDurationPolicy.isAllowed(Duration.ofMinutes(19).plusSeconds(59))).isTrue();
    }

    @Test
    void allowsExactlyTwentyMinutes() {
        assertThat(SongDurationPolicy.isAllowed(Duration.ofMinutes(20))).isTrue();
    }

    @Test
    void rejectsDurationAboveTwentyMinutes() {
        assertThatThrownBy(() -> SongDurationPolicy.requireAllowed(Duration.ofMinutes(20).plusMillis(1)))
                .isInstanceOf(SongDurationLimitExceededException.class)
                .hasMessage("Esse vídeo é longo demais para a rádio. Escolha uma faixa de até 20 minutos.");
    }

    @Test
    void rejectsUnknownDuration() {
        assertThat(SongDurationPolicy.isAllowed(null)).isFalse();
    }
}
