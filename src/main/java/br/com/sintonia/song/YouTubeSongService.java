package br.com.sintonia.song;

import br.com.sintonia.youtube.YouTubeClient;
import br.com.sintonia.youtube.YouTubeSearchResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class YouTubeSongService {

    private final YouTubeClient youTubeClient;
    private final SongService songService;

    public YouTubeSongService(YouTubeClient youTubeClient, SongService songService) {
        this.youTubeClient = youTubeClient;
        this.songService = songService;
    }

    public YouTubeSearchResponse search(String query, int maxResults) {
        return youTubeClient.search(query, maxResults);
    }

    public Optional<Song> select(String videoId) {
        Optional<Song> existing = songService.findByYoutubeVideoId(videoId);
        if (existing.isPresent()) {
            return existing;
        }
        return youTubeClient.getVideoDetails(videoId)
                .map(details -> songService.findOrCreate(
                        details.videoId(),
                        details.title(),
                        details.thumbnailUrl(),
                        details.duration()));
    }
}
