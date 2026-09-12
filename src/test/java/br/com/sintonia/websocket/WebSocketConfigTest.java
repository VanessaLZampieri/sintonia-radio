package br.com.sintonia.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WebSocketConfigTest {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void messagingTemplateIsConfigured() {
        assertThat(messagingTemplate).isNotNull();
    }
}