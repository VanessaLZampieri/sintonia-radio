package br.com.sintonia.room;

import br.com.sintonia.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RoomTest {

    @Test
    void newRoomHasNoLifecycleMarks() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);

        assertThat(room.getEmptySince()).isNull();
        assertThat(room.getClosedAt()).isNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
    }

    @Test
    void markEmptySetsEmptySinceAndKeepsActive() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);

        room.markEmpty();

        assertThat(room.getEmptySince()).isNotNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(room.getClosedAt()).isNull();
    }

    @Test
    void markOccupiedClearsEmptySince() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        room.markEmpty();

        room.markOccupied();

        assertThat(room.getEmptySince()).isNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
    }

    @Test
    void closeMarksClosedAndClearsEmptySince() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        room.markEmpty();

        room.close();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
        assertThat(room.getClosedAt()).isNotNull();
        assertThat(room.getEmptySince()).isNull();
    }

    @Test
    void newRoomHasNoPlayer() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);

        assertThat(room.getPlayerClientSessionId()).isNull();
        assertThat(room.getPlayerUser()).isNull();
        assertThat(room.getPlayerAssumedAt()).isNull();
    }

    @Test
    void claimSetsPlayerFields() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        User user = mock(User.class);

        room.claim("session-1", user);

        assertThat(room.getPlayerClientSessionId()).isEqualTo("session-1");
        assertThat(room.getPlayerUser()).isSameAs(user);
        assertThat(room.getPlayerAssumedAt()).isNotNull();
    }

    @Test
    void releaseClearsPlayerFields() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        room.claim("session-1", mock(User.class));

        room.release();

        assertThat(room.getPlayerClientSessionId()).isNull();
        assertThat(room.getPlayerUser()).isNull();
        assertThat(room.getPlayerAssumedAt()).isNull();
    }
}
