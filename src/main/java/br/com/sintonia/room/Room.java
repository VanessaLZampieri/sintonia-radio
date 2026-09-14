package br.com.sintonia.room;

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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rooms_code", columnNames = "code")
        }
)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 8)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "empty_since")
    private Instant emptySince;

    @Column(name = "player_client_session_id")
    private String playerClientSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_user_id")
    private User playerUser;

    @Column(name = "player_assumed_at")
    private Instant playerAssumedAt;

    public Room(String code, RoomStatus status) {
        this.code = code;
        this.status = status;
    }

    protected Room() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getEmptySince() {
        return emptySince;
    }

    public String getPlayerClientSessionId() {
        return playerClientSessionId;
    }

    public User getPlayerUser() {
        return playerUser;
    }

    public Instant getPlayerAssumedAt() {
        return playerAssumedAt;
    }

    public void markEmpty() {
        this.emptySince = Instant.now();
    }

    public void markOccupied() {
        this.emptySince = null;
    }

    public void close() {
        this.status = RoomStatus.CLOSED;
        this.closedAt = Instant.now();
        this.emptySince = null;
    }

    public void claim(String clientSessionId, User user) {
        this.playerClientSessionId = clientSessionId;
        this.playerUser = user;
        this.playerAssumedAt = Instant.now();
    }

    public void release() {
        this.playerClientSessionId = null;
        this.playerUser = null;
        this.playerAssumedAt = null;
    }
}
