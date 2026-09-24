package br.com.sintonia.room;

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
        name = "room_presences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_presences_room_user_client",
                columnNames = {"room_id", "user_id", "client_session_id"})
)
public class RoomPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_member_id", nullable = false)
    private RoomMember member;

    @Column(name = "client_session_id", nullable = false, length = 36)
    private String clientSessionId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RoomPresence(Room room, User user, RoomMember member, String clientSessionId, Instant expiresAt) {
        this.room = Objects.requireNonNull(room, "room não pode ser nula");
        this.user = Objects.requireNonNull(user, "user não pode ser nulo");
        this.member = Objects.requireNonNull(member, "member não pode ser nulo");
        this.clientSessionId = Objects.requireNonNull(clientSessionId, "clientSessionId não pode ser nulo");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt não pode ser nulo");
    }

    protected RoomPresence() {
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getUser() {
        return user;
    }

    public RoomMember getMember() {
        return member;
    }

    public String getClientSessionId() {
        return clientSessionId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void renew(Instant expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt não pode ser nulo");
    }
}
