package br.com.sintonia.websocket;

import br.com.sintonia.room.PlaybackMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaybackModeChangedEventPayloadTest {

    @Test
    void createsPayload() {
        PlaybackModeChangedEventPayload payload = new PlaybackModeChangedEventPayload(PlaybackMode.CAIXA_DE_MUSICA);

        assertThat(payload.mode()).isEqualTo(PlaybackMode.CAIXA_DE_MUSICA);
    }

    @Test
    void rejectsNullMode() {
        assertThatThrownBy(() -> new PlaybackModeChangedEventPayload(null))
                .isInstanceOf(NullPointerException.class);
    }
}
