package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "playbacks")
public class Playback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "queue_item_id", nullable = false)
    private QueueItem queueItem;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaybackStatus status;

    public Playback(QueueItem queueItem, Instant startedAt) {
        this.queueItem = Objects.requireNonNull(queueItem, "queueItem não pode ser nulo");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt não pode ser nulo");
        this.status = PlaybackStatus.PLAYING;
        this.endedAt = null;
    }

    protected Playback() {
    }

    public Long getId() {
        return id;
    }

    public QueueItem getQueueItem() {
        return queueItem;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public PlaybackStatus getStatus() {
        return status;
    }

    public void setStatus(PlaybackStatus status) {
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = Objects.requireNonNull(endedAt, "endedAt não pode ser nulo");
    }
}