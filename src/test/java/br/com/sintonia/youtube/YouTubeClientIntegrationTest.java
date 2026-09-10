package br.com.sintonia.youtube;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "YOUTUBE_API_KEY", matches = ".+")
class YouTubeClientIntegrationTest {

    @Autowired
    private YouTubeClient youTubeClient;

    @Test
    void searchesVideosOnYouTube() {
        YouTubeSearchResponse response = youTubeClient.search("never gonna give you up", 5);

        assertThat(response).isNotNull();
        assertThat(response.items()).isNotNull();
    }
}
