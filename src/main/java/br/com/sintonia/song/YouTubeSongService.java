package br.com.sintonia.song;

import br.com.sintonia.youtube.YouTubeClient;
import br.com.sintonia.youtube.YouTubeSearchItem;
import br.com.sintonia.youtube.YouTubeSearchResponse;
import br.com.sintonia.youtube.YouTubeVideoDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class YouTubeSongService {

    private final YouTubeClient youTubeClient;
    private final SongService songService;

    public YouTubeSongService(YouTubeClient youTubeClient, SongService songService) {
        this.youTubeClient = youTubeClient;
        this.songService = songService;
    }

    public List<SongSearchItemResponse> search(String query, int maxResults) {
        YouTubeSearchResponse response = youTubeClient.search(query, maxResults);
        if (response == null || response.items() == null) {
            return List.of();
        }

        List<String> videoIds = response.items().stream()
                .map(item -> item.id() == null ? null : item.id().videoId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, YouTubeVideoDetails> details = youTubeClient.getVideoDetailsBatch(videoIds);

        Set<String> seen = new HashSet<>();
        List<SongSearchItemResponse> results = new ArrayList<>();
        for (YouTubeSearchItem item : response.items()) {
            String videoId = item.id() == null ? null : item.id().videoId();
            if (videoId == null || !seen.add(videoId)) {
                continue;
            }
            YouTubeVideoDetails videoDetails = details.get(videoId);
            if (videoDetails == null || !videoDetails.embeddable() || videoDetails.live()) {
                continue;
            }
            results.add(SongSearchItemResponse.from(item, videoDetails));
        }
        return results;
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
