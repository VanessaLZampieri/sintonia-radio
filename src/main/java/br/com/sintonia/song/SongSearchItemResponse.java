package br.com.sintonia.song;

import br.com.sintonia.youtube.YouTubeSearchItem;

public record SongSearchItemResponse(String videoId, String title) {

    public static SongSearchItemResponse from(YouTubeSearchItem item) {
        return new SongSearchItemResponse(
                item.id() == null ? null : item.id().videoId(),
                item.snippet() == null ? null : item.snippet().title());
    }
}
