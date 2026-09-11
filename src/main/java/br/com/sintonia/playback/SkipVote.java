package br.com.sintonia.playback;

import br.com.sintonia.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "skip_votes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_skip_votes_playback_user", columnNames = {"playback_id", "user_id"})
        }
)
public class SkipVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playback_id", nullable = false)
    private Playback playback;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SkipVote(Playback playback, User user, Instant createdAt) {
        this.playback = Objects.requireNonNull(playback, "playback não pode ser nulo");
        this.user = Objects.requireNonNull(user, "user não pode ser nulo");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt não pode ser nulo");
    }

    protected SkipVote() {
    }

    public Long getId() {
        return id;
    }

    public Playback getPlayback() {
        return playback;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}