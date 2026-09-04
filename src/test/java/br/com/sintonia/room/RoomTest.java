package br.com.sintonia.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
