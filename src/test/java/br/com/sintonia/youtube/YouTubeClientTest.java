package br.com.sintonia.youtube;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YouTubeClientTest {

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    private MockRestServiceServer server;
    private YouTubeClient youTubeClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        youTubeClient = new YouTubeClient(builder.build(), "test-api-key");
    }

    @Test
    void convertsIso8601Duration() {
        String url = BASE_URL + "/videos?part=snippet,contentDetails&id=abc123&key=test-api-key";
        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"items\":[{\"id\":\"abc123\","
                                + "\"snippet\":{\"title\":\"T\",\"thumbnails\":{\"medium\":{\"url\":\"https://img/1.jpg\"}}},"
                                + "\"contentDetails\":{\"duration\":\"PT4M13S\"}}]}",
                        MediaType.APPLICATION_JSON));

        Optional<YouTubeVideoDetails> result = youTubeClient.getVideoDetails("abc123");

        assertThat(result).isPresent();
        assertThat(result.get().duration()).isEqualTo(Duration.ofMinutes(4).plusSeconds(13));
    }

    @Test
    void convertsDurationWithHours() {
        String url = BASE_URL + "/videos?part=snippet,contentDetails&id=xyz789&key=test-api-key";
        server.expect(requestTo(url))
                .andRespond(withSuccess(
                        "{\"items\":[{\"id\":\"xyz789\","
                                + "\"snippet\":{\"title\":\"T\",\"thumbnails\":{\"medium\":{\"url\":\"https://img/1.jpg\"}}},"
                                + "\"contentDetails\":{\"duration\":\"PT1H2M30S\"}}]}",
                        MediaType.APPLICATION_JSON));

        Optional<YouTubeVideoDetails> result = youTubeClient.getVideoDetails("xyz789");

        assertThat(result).isPresent();
        assertThat(result.get().duration()).isEqualTo(Duration.ofHours(1).plusMinutes(2).plusSeconds(30));
    }

    @Test
    void returnsVideoDetailsWhenFound() {
        String url = BASE_URL + "/videos?part=snippet,contentDetails&id=abc123&key=test-api-key";
        server.expect(requestTo(url))
                .andRespond(withSuccess(
                        "{\"items\":[{\"id\":\"abc123\","
                                + "\"snippet\":{\"title\":\"Some Title\",\"thumbnails\":{\"medium\":{\"url\":\"https://img/1.jpg\"}}},"
                                + "\"contentDetails\":{\"duration\":\"PT4M13S\"}}]}",
                        MediaType.APPLICATION_JSON));

        Optional<YouTubeVideoDetails> result = youTubeClient.getVideoDetails("abc123");

        assertThat(result).isPresent();
        assertThat(result.get().videoId()).isEqualTo("abc123");
        assertThat(result.get().title()).isEqualTo("Some Title");
        assertThat(result.get().thumbnailUrl()).isEqualTo("https://img/1.jpg");
        assertThat(result.get().duration()).isEqualTo(Duration.ofMinutes(4).plusSeconds(13));
    }

    @Test
    void returnsEmptyWhenVideoNotFound() {
        String url = BASE_URL + "/videos?part=snippet,contentDetails&id=missing&key=test-api-key";
        server.expect(requestTo(url))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        Optional<YouTubeVideoDetails> result = youTubeClient.getVideoDetails("missing");

        assertThat(result).isEmpty();
    }
}
