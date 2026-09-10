package br.com.sintonia.song;

public record SongResponse(Long id, String youtubeVideoId, String title, String thumbnailUrl, String duration) {

    public static SongResponse from(Song song) {
        return new SongResponse(
                song.getId(),
                song.getYoutubeVideoId(),
                song.getTitle(),
                song.getThumbnailUrl(),
                song.getDuration().toString());
    }
}
