package br.com.sintonia.youtube;

public record YouTubeSearchItem(ItemId id, Snippet snippet) {

    public record ItemId(String videoId) {
    }

    public record Snippet(String title) {
    }
}
