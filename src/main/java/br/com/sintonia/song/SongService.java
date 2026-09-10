package br.com.sintonia.song;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class SongService {

    private final SongRepository songRepository;

    public SongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    public Optional<Song> findByYoutubeVideoId(String youtubeVideoId) {
        return songRepository.findByYoutubeVideoId(youtubeVideoId);
    }

    public Song findOrCreate(String youtubeVideoId, String title, String thumbnailUrl, Duration duration) {
        return songRepository.findByYoutubeVideoId(youtubeVideoId)
                .orElseGet(() -> songRepository.save(new Song(youtubeVideoId, title, thumbnailUrl, duration)));
    }
}
