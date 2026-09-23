package br.com.sintonia.youtube;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    void searchIncludesEmbeddableFilter() {
        String url = BASE_URL + "/search?part=snippet&type=video&videoEmbeddable=true&q=foo&maxResults=5&key=test-api-key";
        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        YouTubeSearchResponse result = youTubeClient.search("foo", 5);

        assertThat(result).isNotNull();
        assertThat(result.items()).isEmpty();
    }

    @Test
    void convertsIso8601DurationAndParsesStatus() {
        String url = BASE_URL + "/videos?part=snippet,contentDetails,status&id=abc123&key=test-api-key";
        server.expect(requestTo(url))
                .andRespond(withSuccess(
                        "{\"items\":[{\"id\":\"abc123\","
                                + "\"snippet\":{\"title\":\"T\",\"channelTitle\":\"C\","
                                + "\"thumbnails\":{\"medium\":{\"url\":\"https://img/1.jpg\"}}},"
                                + "\"contentDetails\":{\"duration\":\"PT4M13S\"},"
                                + "\"status\":{\"embeddable\":true,\"liveBroadcastContent\":\"none\"}}]}",
                        MediaType.APPLICATION_JSON));

        Optional<YouTubeVideoDetails> result = youTubeClient.getVideoDetails("abc123");

        assertThat(result).isPresent();
        assertThat(result.get().duration()).isEqualTo(Duration.ofMinutes(4).plusSeconds(13));
        assertThat(result.get().channelTitle()).isEqualTo("C");
        assertThat(result.get().embeddable()).isTrue();
        assertThat(result.get().live()).isFalse();
    }

    @Test
    void batchReturnsOnlyItemsWithValidDuration() {
        String url = BASE_URL + "/videos?part=snippet,contentDetails,status&id=abc123,missing&key=test-api-key";
        server.expect(requestTo(url))
                .andRespond(withSuccess(
                        "{\"items\":["
                                + "{\"id\":\"abc123\",\"snippet\":{\"title\":\"T\",\"channelTitle\":\"C\","
                                + "\"thumbnails\":{\"medium\":{\"url\":\"https://img/1.jpg\"}}},"
                                + "\"contentDetails\":{\"duration\":\"PT4M13S\"},"
                                + "\"status\":{\"embeddable\":true,\"liveBroadcastContent\":\"none\"}},"
                                + "{\"id\":\"missing\",\"snippet\":{\"title\":\"NoDur\"},"
                                + "\"contentDetails\":{\"duration\":null},"
                                + "\"status\":{\"embeddable\":true,\"liveBroadcastContent\":\"none\"}}"
                                + "]}",
                        MediaType.APPLICATION_JSON));

        Map<String, YouTubeVideoDetails> result = youTubeClient.getVideoDetailsBatch(List.of("abc123", "missing"));

        assertThat(result).containsOnlyKeys("abc123");
    }

    @Test
    void batchParsesLiveStatus() {
        String url = BASE_URL + "/videos?part=snippet,contentDetails,status&id=live1&key=test-api-key";
        server.expect(requestTo(url))
                .andRespond(withSuccess(
                        "{\"items\":[{\"id\":\"live1\",\"snippet\":{\"title\":\"Live\"},"
                                + "\"contentDetails\":{\"duration\":\"PT1H\"},"
                                + "\"status\":{\"embeddable\":true,\"liveBroadcastContent\":\"live\"}}]}",
                        MediaType.APPLICATION_JSON));

        Map<String, YouTubeVideoDetails> result = youTubeClient.getVideoDetailsBatch(List.of("live1"));

        assertThat(result.get("live1").live()).isTrue();
    }

    @Test
    void returnsEmptyWhenVideoNotFound() {
        String url = BASE_URL + "/videos?part=snippet,contentDetails,status&id=missing&key=test-api-key";
        server.expect(requestTo(url))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        Optional<YouTubeVideoDetails> result = youTubeClient.getVideoDetails("missing");

        assertThat(result).isEmpty();
    }
}
