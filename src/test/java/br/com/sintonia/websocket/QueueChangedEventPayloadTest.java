package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueChangedEventPayloadTest {

    @Test
    void createsValidPayload() {
        QueueChangedEventPayload payload = new QueueChangedEventPayload(42L, QueueChangeAction.ADDED);

        assertThat(payload.queueItemId()).isEqualTo(42L);
        assertThat(payload.action()).isEqualTo(QueueChangeAction.ADDED);
    }

    @Test
    void rejectsNullQueueItemId() {
        assertThatThrownBy(() -> new QueueChangedEventPayload(null, QueueChangeAction.ADDED))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullAction() {
        assertThatThrownBy(() -> new QueueChangedEventPayload(42L, null))
                .isInstanceOf(NullPointerException.class);
    }
}
