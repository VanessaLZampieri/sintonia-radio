package br.com.sintonia.playback;

import br.com.sintonia.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SkipVoteTest {

    private final Playback playback = mock(Playback.class);
    private final User user = mock(User.class);
    private final Instant createdAt = Instant.parse("2026-01-01T19:30:00Z");

    @Test
    void createsValidSkipVote() {
        SkipVote vote = new SkipVote(playback, user, createdAt);

        assertThat(vote.getPlayback()).isSameAs(playback);
        assertThat(vote.getUser()).isSameAs(user);
        assertThat(vote.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void rejectsNullPlayback() {
        assertThatThrownBy(() -> new SkipVote(null, user, createdAt))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullUser() {
        assertThatThrownBy(() -> new SkipVote(playback, null, createdAt))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> new SkipVote(playback, user, null))
                .isInstanceOf(NullPointerException.class);
    }
}