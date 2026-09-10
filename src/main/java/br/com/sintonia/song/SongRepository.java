package br.com.sintonia.song;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findByYoutubeVideoId(String youtubeVideoId);
}
