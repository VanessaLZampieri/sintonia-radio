package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkipVoteChangedEventPayloadTest {

    @Test
    void createsPayload() {
        SkipVoteChangedEventPayload payload = new SkipVoteChangedEventPayload(1L, 3L, 5L);

        assertThat(payload.playbackId()).isEqualTo(1L);
        assertThat(payload.votes()).isEqualTo(3L);
        assertThat(payload.requiredVotes()).isEqualTo(5L);
    }
}
