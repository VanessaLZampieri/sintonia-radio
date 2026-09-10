package br.com.sintonia.youtube;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class YouTubeClientConfigTest {

    @Autowired
    private YouTubeClient youTubeClient;

    @Test
    void clientIsCreatedBySpring() {
        assertThat(youTubeClient).isNotNull();
    }
}
