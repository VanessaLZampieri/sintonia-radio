package br.com.sintonia.room;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomLifecycleServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @InjectMocks
    private RoomLifecycleService roomLifecycleService;

    @Test
    void closesRoomEmptyForMoreThanTwentyMinutes() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        setEmptySince(room, Instant.now().minus(25, ChronoUnit.MINUTES));

        when(roomRepository.findExpiredCandidateIds(eq(RoomStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(1L));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(0L);

        roomLifecycleService.closeExpiredRooms();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
        assertThat(room.getClosedAt()).isNotNull();
        assertThat(room.getEmptySince()).isNull();
    }

    @Test
    void doesNotCloseRoomWithActiveParticipants() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        setEmptySince(room, Instant.now().minus(25, ChronoUnit.MINUTES));

        when(roomRepository.findExpiredCandidateIds(eq(RoomStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(1L));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(1L);

        roomLifecycleService.closeExpiredRooms();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(room.getClosedAt()).isNull();
    }

    @Test
    void doesNotCloseRoomThatIsAlreadyClosed() {
        Room room = new Room("ABCDEFGH", RoomStatus.CLOSED);
        setEmptySince(room, Instant.now().minus(25, ChronoUnit.MINUTES));

        when(roomRepository.findExpiredCandidateIds(eq(RoomStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(1L));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));

        roomLifecycleService.closeExpiredRooms();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    }

    @Test
    void doesNotCloseRoomThatBecameEmptyLessThanTwentyMinutesAgo() {
        Room room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        setEmptySince(room, Instant.now().minus(10, ChronoUnit.MINUTES));

        when(roomRepository.findExpiredCandidateIds(eq(RoomStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(1L));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));

        roomLifecycleService.closeExpiredRooms();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
    }

    private void setEmptySince(Room room, Instant instant) {
        ReflectionTestUtils.setField(room, "emptySince", instant);
    }
}
