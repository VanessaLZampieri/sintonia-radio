package br.com.sintonia.youtube;

import java.time.Duration;

public record YouTubeVideoDetails(
        String videoId,
        String title,
        String channelTitle,
        String thumbnailUrl,
        Duration duration,
        boolean embeddable,
        boolean live) {
}
