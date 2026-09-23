package br.com.sintonia.room;

import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomActivityServiceTest {

    private static final Long ROOM_ID = 1L;

    @Mock
    private RoomActivityRepository roomActivityRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomActivityService roomActivityService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
    }

    @Test
    void recordCopiesActorAndSongSnapshot() {
        User user = new User("g1", "Vanessa Souza", "vanessa@example.com", null);
        ReflectionTestUtils.setField(user, "id", 10L);
        Song song = new Song("vid", "Everlong", null, Duration.ofSeconds(200));

        roomActivityService.record(room, RoomActivityType.SONG_ADDED, user, song, null);

        ArgumentCaptor<RoomActivity> captor = ArgumentCaptor.forClass(RoomActivity.class);
        verify(roomActivityRepository).save(captor.capture());
        RoomActivity saved = captor.getValue();

        assertThat(saved.getType()).isEqualTo(RoomActivityType.SONG_ADDED);
        assertThat(saved.getActorUserId()).isEqualTo(10L);
        assertThat(saved.getActorDisplayName()).isEqualTo("Vanessa");
        assertThat(saved.getSongTitle()).isEqualTo("Everlong");

        user.updateDisplayName("Van");
        assertThat(saved.getActorDisplayName()).isEqualTo("Vanessa");
    }

    @Test
    void findRecentReturnsMappedActivitiesAndLimitsToFifty() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        RoomActivity activity = new RoomActivity(room, RoomActivityType.MEMBER_JOINED, 10L, "Ana", null, null, null);
        ReflectionTestUtils.setField(activity, "id", 1L);
        when(roomActivityRepository.findByRoomIdOrderByCreatedAtDescIdDesc(eq(ROOM_ID), any(Pageable.class)))
                .thenReturn(List.of(activity));

        List<RoomActivityResponse> result = roomActivityService.findRecent(ROOM_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(RoomActivityType.MEMBER_JOINED);
        assertThat(result.get(0).actorDisplayName()).isEqualTo("Ana");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(roomActivityRepository).findByRoomIdOrderByCreatedAtDescIdDesc(eq(ROOM_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
    }

    @Test
    void findRecentAllowsClosedRoom() {
        Room closed = new Room("Sala Fechada", "FECHADO1", RoomStatus.CLOSED);
        ReflectionTestUtils.setField(closed, "id", 5L);
        when(roomRepository.findById(5L)).thenReturn(Optional.of(closed));
        when(roomActivityRepository.findByRoomIdOrderByCreatedAtDescIdDesc(eq(5L), any(Pageable.class)))
                .thenReturn(List.of());

        assertThat(roomActivityService.findRecent(5L)).isEmpty();
    }

    @Test
    void findRecentRejectsUnknownRoom() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomActivityService.findRecent(ROOM_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }
}
