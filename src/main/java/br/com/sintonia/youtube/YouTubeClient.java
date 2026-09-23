package br.com.sintonia.youtube;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                        .queryParam("videoEmbeddable", "true")
                        .queryParam("q", query)
                        .queryParam("maxResults", maxResults)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(YouTubeSearchResponse.class);
    }

    public Map<String, YouTubeVideoDetails> getVideoDetailsBatch(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return Map.of();
        }

        YouTubeVideoListResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet,contentDetails,status")
                        .queryParam("id", String.join(",", videoIds))
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(YouTubeVideoListResponse.class);

        if (response == null || response.items() == null) {
            return Map.of();
        }

        Map<String, YouTubeVideoDetails> details = new LinkedHashMap<>();
        for (YouTubeVideoListResponse.Item item : response.items()) {
            YouTubeVideoDetails videoDetails = toDetails(item);
            if (videoDetails != null) {
                details.put(videoDetails.videoId(), videoDetails);
            }
        }
        return details;
    }

    public Optional<YouTubeVideoDetails> getVideoDetails(String videoId) {
        return getVideoDetailsBatch(List.of(videoId)).values().stream().findFirst();
    }

    private YouTubeVideoDetails toDetails(YouTubeVideoListResponse.Item item) {
        String id = item.id();
        String title = item.snippet() == null ? null : item.snippet().title();
        String channelTitle = item.snippet() == null ? null : item.snippet().channelTitle();
        String thumbnailUrl = thumbnailUrl(item.snippet());

        String durationString = item.contentDetails() == null ? null : item.contentDetails().duration();
        Duration duration = parseDuration(durationString);

        boolean embeddable = item.status() != null && Boolean.TRUE.equals(item.status().embeddable());
        boolean live = item.status() != null
                && ("live".equals(item.status().liveBroadcastContent())
                        || "upcoming".equals(item.status().liveBroadcastContent()));

        if (id == null || duration == null) {
            return null;
        }

        return new YouTubeVideoDetails(id, title, channelTitle, thumbnailUrl, duration, embeddable, live);
    }

    private Duration parseDuration(String durationString) {
        if (durationString == null) {
            return null;
        }
        try {
            return Duration.parse(durationString);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String thumbnailUrl(YouTubeVideoListResponse.Snippet snippet) {
        if (snippet == null || snippet.thumbnails() == null || snippet.thumbnails().medium() == null) {
            return null;
        }
        return snippet.thumbnails().medium().url();
    }
}
