package br.com.sintonia.youtube;

import java.util.List;

public record YouTubeVideoListResponse(List<Item> items) {

    public record Item(String id, Snippet snippet, ContentDetails contentDetails, Status status) {
    }

    public record Snippet(String title, String channelTitle, Thumbnails thumbnails) {
    }

    public record Thumbnails(Thumbnail medium) {
    }

    public record Thumbnail(String url) {
    }

    public record ContentDetails(String duration) {
    }

    public record Status(Boolean embeddable, String liveBroadcastContent) {
    }
}
