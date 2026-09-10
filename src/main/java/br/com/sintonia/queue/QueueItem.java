package br.com.sintonia.queue;

import br.com.sintonia.room.Room;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
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
@Table(name = "queue_items")
public class QueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @Column(nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueItemStatus status;

    public QueueItem(Room room, Song song, User user, Instant addedAt, Integer position) {
        this.room = Objects.requireNonNull(room, "room não pode ser nulo");
        this.song = Objects.requireNonNull(song, "song não pode ser nulo");
        this.user = Objects.requireNonNull(user, "user não pode ser nulo");
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt não pode ser nulo");
        this.position = Objects.requireNonNull(position, "position não pode ser nulo");
        if (position < 1) {
            throw new IllegalArgumentException("position deve ser maior ou igual a 1");
        }
        this.status = QueueItemStatus.WAITING;
    }

    protected QueueItem() {
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public Song getSong() {
        return song;
    }

    public User getUser() {
        return user;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public Integer getPosition() {
        return position;
    }

    public QueueItemStatus getStatus() {
        return status;
    }

    public void setStatus(QueueItemStatus status) {
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
    }
}
