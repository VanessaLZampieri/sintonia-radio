package br.com.sintonia.youtube;

import java.util.List;

public record YouTubeSearchResponse(List<YouTubeSearchItem> items) {
}
