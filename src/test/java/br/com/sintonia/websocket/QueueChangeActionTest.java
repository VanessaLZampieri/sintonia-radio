package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueueChangeActionTest {

    @Test
    void hasExactlyAddedAndRemoved() {
        assertThat(QueueChangeAction.values())
                .containsExactly(QueueChangeAction.ADDED, QueueChangeAction.REMOVED);
    }
}
