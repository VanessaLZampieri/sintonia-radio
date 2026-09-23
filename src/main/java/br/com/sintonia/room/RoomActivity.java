package br.com.sintonia.room;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "room_activities")
public class RoomActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomActivityType type;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_display_name")
    private String actorDisplayName;

    @Column(name = "song_id")
    private Long songId;

    @Column(name = "song_title")
    private String songTitle;

    @Column
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RoomActivity(Room room, RoomActivityType type, Long actorUserId, String actorDisplayName,
                        Long songId, String songTitle, String detail) {
        this.room = Objects.requireNonNull(room, "room não pode ser nulo");
        this.type = Objects.requireNonNull(type, "type não pode ser nulo");
        this.actorUserId = actorUserId;
        this.actorDisplayName = actorDisplayName;
        this.songId = songId;
        this.songTitle = songTitle;
        this.detail = detail;
    }

    protected RoomActivity() {
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public RoomActivityType getType() {
        return type;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public Long getSongId() {
        return songId;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
