package br.com.sintonia.room;

import br.com.sintonia.playback.PlaybackRepository;
import br.com.sintonia.playback.SkipVoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomSummaryServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private RoomActivityRepository roomActivityRepository;

    @Mock
    private PlaybackRepository playbackRepository;

    @Mock
    private SkipVoteRepository skipVoteRepository;

    @InjectMocks
    private RoomSummaryService roomSummaryService;

    @Test
    void rejectsUserWhoNeverParticipatedInRoom() {
        Room room = new Room("Sala", "SUMMARY1", RoomStatus.ACTIVE);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserId(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> roomSummaryService.get(1L, 99L))
                .isInstanceOf(UserNotInRoomException.class);

        verifyNoInteractions(roomActivityRepository, playbackRepository, skipVoteRepository);
    }
}
