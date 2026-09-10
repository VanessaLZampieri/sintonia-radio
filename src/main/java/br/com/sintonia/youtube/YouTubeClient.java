package br.com.sintonia.youtube;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

@Component
public class YouTubeClient {

    private final RestClient restClient;
    private final String apiKey;

    public YouTubeClient(RestClient restClient, @Value("${youtube.api-key}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public YouTubeSearchResponse search(String query, int maxResults) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("type", "video")
                        .queryParam("q", query)
                        .queryParam("maxResults", maxResults)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(YouTubeSearchResponse.class);
    }

    public Optional<YouTubeVideoDetails> getVideoDetails(String videoId) {
        YouTubeVideoListResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet,contentDetails")
                        .queryParam("id", videoId)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(YouTubeVideoListResponse.class);

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return Optional.empty();
        }

        YouTubeVideoListResponse.Item item = response.items().get(0);
        if (item.contentDetails() == null || item.contentDetails().duration() == null) {
            return Optional.empty();
        }

        String returnedId = item.id() == null ? videoId : item.id();
        String title = item.snippet() == null ? null : item.snippet().title();
        String thumbnailUrl = thumbnailUrl(item.snippet());
        Duration duration = Duration.parse(item.contentDetails().duration());

        return Optional.of(new YouTubeVideoDetails(returnedId, title, thumbnailUrl, duration));
    }

    private String thumbnailUrl(YouTubeVideoListResponse.Snippet snippet) {
        if (snippet == null || snippet.thumbnails() == null || snippet.thumbnails().medium() == null) {
            return null;
        }
        return snippet.thumbnails().medium().url();
    }
}
