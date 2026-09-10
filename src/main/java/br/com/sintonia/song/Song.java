package br.com.sintonia.song;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Duration;
import java.util.Objects;

@Entity
@Table(
        name = "songs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_songs_youtube_video_id", columnNames = "youtube_video_id")
        }
)
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "youtube_video_id", nullable = false)
    private String youtubeVideoId;

    @Column(nullable = false)
    private String title;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration", nullable = false)
    private Duration duration;

    public Song(String youtubeVideoId, String title, String thumbnailUrl, Duration duration) {
        this.youtubeVideoId = Objects.requireNonNull(youtubeVideoId, "youtubeVideoId não pode ser nulo");
        this.title = Objects.requireNonNull(title, "title não pode ser nulo");
        this.thumbnailUrl = thumbnailUrl;
        this.duration = Objects.requireNonNull(duration, "duration não pode ser nulo");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration não pode ser negativa");
        }
    }

    protected Song() {
    }

    public Long getId() {
        return id;
    }

    public String getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Duration getDuration() {
        return duration;
    }
}
