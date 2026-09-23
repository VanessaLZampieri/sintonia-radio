package br.com.sintonia.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_google_id", columnNames = "google_id"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", nullable = false, unique = true)
    private String googleId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String googleId, String name, String email, String avatarUrl) {
        this.googleId = Objects.requireNonNull(googleId, "googleId não pode ser nulo");
        this.name = Objects.requireNonNull(name, "name não pode ser nulo");
        this.email = Objects.requireNonNull(email, "email não pode ser nulo");
        this.avatarUrl = avatarUrl;
        this.displayName = firstWordOf(this.name);
    }

    public void updateProfile(String name, String email, String avatarUrl) {
        this.name = Objects.requireNonNull(name, "name não pode ser nulo");
        this.email = Objects.requireNonNull(email, "email não pode ser nulo");
        this.avatarUrl = avatarUrl;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName não pode ser nulo").trim();
    }

    private static String firstWordOf(String name) {
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space == -1 ? trimmed : trimmed.substring(0, space);
    }

    public Long getId() {
        return id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
