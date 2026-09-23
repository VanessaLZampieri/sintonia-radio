package br.com.sintonia.room;

public record ActiveRoomResponse(
        Long roomId,
        String name,
        String code,
        long participantCount,
        long waitingCount,
        NowPlaying nowPlaying) {

    public record NowPlaying(
            String title,
            String youtubeVideoId,
            String thumbnailUrl,
            AddedBy addedBy) {
    }

    public record AddedBy(Long userId, String displayName, String avatarUrl) {
    }
}
