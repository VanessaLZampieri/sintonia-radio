package br.com.sintonia.song;

import br.com.sintonia.youtube.YouTubeSearchItem;
import br.com.sintonia.youtube.YouTubeVideoDetails;

public record SongSearchItemResponse(String videoId, String title, String channelTitle, String duration) {

    public static SongSearchItemResponse from(YouTubeSearchItem item, YouTubeVideoDetails details) {
        return new SongSearchItemResponse(
                item.id() == null ? null : item.id().videoId(),
                item.snippet() == null ? null : item.snippet().title(),
                details == null ? null : details.channelTitle(),
                details == null || details.duration() == null ? null : details.duration().toString());
    }
}
