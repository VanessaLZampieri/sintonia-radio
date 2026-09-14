package br.com.sintonia.playback;

import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomMemberRepository;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import br.com.sintonia.websocket.RoomEventType;
import br.com.sintonia.websocket.SkipVoteChangedEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkipVoteServiceTest {

    private static final Long ROOM_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Long PLAYBACK_ID = 1L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PlaybackRepository playbackRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private SkipVoteRepository skipVoteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaybackService playbackService;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    @InjectMocks
    private SkipVoteService skipVoteService;

    private Room room;
    private Playback playback;
    private User user;

    @BeforeEach
    void setUp() {
        room = new Room("ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        playback = mock(Playback.class);
        user = mock(User.class);
    }

    @Test
    void voteRegistersBelowThreshold() {
        stubVotePath(5, 1);

        SkipVoteResponse result = skipVoteService.vote(ROOM_ID, USER_ID);

        assertThat(result.playbackId()).isEqualTo(PLAYBACK_ID);
        assertThat(result.votes()).isEqualTo(1);
        assertThat(result.requiredVotes()).isEqualTo(3);
        assertThat(result.skipped()).isFalse();
        verify(playbackService, never()).skip(PLAYBACK_ID);
    }

    @Test
    void voteAtThresholdSkips() {
        stubVotePath(5, 3);

        SkipVoteResponse result = skipVoteService.vote(ROOM_ID, USER_ID);

        assertThat(result.skipped()).isTrue();
        verify(playbackService).skip(PLAYBACK_ID);
    }

    @Test
    void voteAboveThresholdSkips() {
        stubVotePath(5, 4);

        SkipVoteResponse result = skipVoteService.vote(ROOM_ID, USER_ID);

        assertThat(result.skipped()).isTrue();
        verify(playbackService).skip(PLAYBACK_ID);
    }

    @Test
    void voteIsAssociatedWithCurrentPlayingPlayback() {
        stubVotePath(5, 1);

        skipVoteService.vote(ROOM_ID, USER_ID);

        ArgumentCaptor<SkipVote> captor = ArgumentCaptor.forClass(SkipVote.class);
        verify(skipVoteRepository).save(captor.capture());
        assertThat(captor.getValue().getPlayback()).isSameAs(playback);
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void votePublishesSkipVoteChangedEvent() {
        stubVotePath(5, 1);

        skipVoteService.vote(ROOM_ID, USER_ID);

        ArgumentCaptor<RoomEvent> captor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(roomEventPublisher).publish(eq("ABCDEFGH"), captor.capture());
        RoomEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RoomEventType.SKIP_VOTE_CHANGED);
        assertThat(event.roomId()).isEqualTo(ROOM_ID);
        SkipVoteChangedEventPayload payload = (SkipVoteChangedEventPayload) event.payload();
        assertThat(payload.playbackId()).isEqualTo(PLAYBACK_ID);
        assertThat(payload.votes()).isEqualTo(1);
        assertThat(payload.requiredVotes()).isEqualTo(3);
    }

    @Test
    void voteRejectsDuplicateVote() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.of(playback));
        when(playback.getId()).thenReturn(PLAYBACK_ID);
        when(skipVoteRepository.existsByPlaybackIdAndUserId(PLAYBACK_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> skipVoteService.vote(ROOM_ID, USER_ID))
                .isInstanceOf(SkipVoteAlreadyExistsException.class);

        verify(skipVoteRepository, never()).save(any(SkipVote.class));
    }

    @Test
    void voteRejectsUserNotInRoom() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> skipVoteService.vote(ROOM_ID, USER_ID))
                .isInstanceOf(UserNotInRoomException.class);

        verifyNoInteractions(roomEventPublisher);
    }

    @Test
    void voteRejectsWhenNoPlayingPlayback() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> skipVoteService.vote(ROOM_ID, USER_ID))
                .isInstanceOf(PlaybackNotFoundException.class);
    }

    @Test
    void voteRejectsUnknownRoom() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skipVoteService.vote(ROOM_ID, USER_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void voteRejectsClosedRoom() {
        Room closedRoom = new Room("ABCDEFGH", RoomStatus.CLOSED);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(closedRoom));

        assertThatThrownBy(() -> skipVoteService.vote(ROOM_ID, USER_ID))
                .isInstanceOf(RoomClosedException.class);
    }

    @Test
    void currentReturnsStateWhenPlaying() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.of(playback));
        when(playback.getId()).thenReturn(PLAYBACK_ID);
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(5L);
        when(skipVoteRepository.countByPlaybackId(PLAYBACK_ID)).thenReturn(2L);

        SkipVoteResponse result = skipVoteService.current(ROOM_ID).orElseThrow();

        assertThat(result.votes()).isEqualTo(2);
        assertThat(result.requiredVotes()).isEqualTo(3);
        assertThat(result.skipped()).isFalse();
    }

    @Test
    void currentEmptyWhenNoPlaying() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());

        assertThat(skipVoteService.current(ROOM_ID)).isEmpty();
    }

    @Test
    void requiredVotesCalculation() {
        assertThat(SkipVoteService.requiredVotes(1)).isEqualTo(1);
        assertThat(SkipVoteService.requiredVotes(2)).isEqualTo(2);
        assertThat(SkipVoteService.requiredVotes(3)).isEqualTo(2);
        assertThat(SkipVoteService.requiredVotes(4)).isEqualTo(3);
        assertThat(SkipVoteService.requiredVotes(5)).isEqualTo(3);
        assertThat(SkipVoteService.requiredVotes(10)).isEqualTo(6);
        assertThat(SkipVoteService.requiredVotes(15)).isEqualTo(9);
    }

    private void stubVotePath(long participants, long votesAfter) {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.of(playback));
        when(playback.getId()).thenReturn(PLAYBACK_ID);
        when(skipVoteRepository.existsByPlaybackIdAndUserId(PLAYBACK_ID, USER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(skipVoteRepository.save(any(SkipVote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMemberRepository.countByRoomAndLeftAtIsNull(room)).thenReturn(participants);
        when(skipVoteRepository.countByPlaybackId(PLAYBACK_ID)).thenReturn(votesAfter);
    }
}
