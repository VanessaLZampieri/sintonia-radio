package br.com.sintonia.youtube;

import java.time.Duration;

public record YouTubeVideoDetails(String videoId, String title, String thumbnailUrl, Duration duration) {
}
