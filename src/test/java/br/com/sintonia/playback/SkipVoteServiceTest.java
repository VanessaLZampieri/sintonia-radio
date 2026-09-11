package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomMemberRepository;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkipVoteServiceTest {

    private static final Long PLAYBACK_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long ROOM_ID = 10L;

    @Mock
    private PlaybackRepository playbackRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private SkipVoteRepository skipVoteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SkipVoteService skipVoteService;

    private Playback playback;
    private QueueItem queueItem;
    private Room room;
    private User user;

    @BeforeEach
    void setUp() {
        playback = mock(Playback.class);
        queueItem = mock(QueueItem.class);
        room = mock(Room.class);
        user = mock(User.class);
    }

    @Test
    void userPresentVotesForPlayingPlayback() {
        stubHappyPath();

        SkipVote result = skipVoteService.vote(PLAYBACK_ID, USER_ID);

        assertThat(result).isNotNull();
    }

    @Test
    void voteIsAssociatedWithCorrectPlayback() {
        stubHappyPath();

        SkipVote result = skipVoteService.vote(PLAYBACK_ID, USER_ID);

        assertThat(result.getPlayback()).isSameAs(playback);
    }

    @Test
    void voteIsAssociatedWithCorrectUser() {
        stubHappyPath();

        SkipVote result = skipVoteService.vote(PLAYBACK_ID, USER_ID);

        assertThat(result.getUser()).isSameAs(user);
    }

    @Test
    void voteHasCreatedAt() {
        stubHappyPath();

        SkipVote result = skipVoteService.vote(PLAYBACK_ID, USER_ID);

        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void voteIsPersisted() {
        stubHappyPath();

        skipVoteService.vote(PLAYBACK_ID, USER_ID);

        verify(skipVoteRepository).save(any(SkipVote.class));
    }

    @Test
    void throwsWhenPlaybackNotFound() {
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skipVoteService.vote(PLAYBACK_ID, USER_ID))
                .isInstanceOf(PlaybackNotFoundException.class);
    }

    @Test
    void throwsWhenPlaybackFinished() {
        stubNotPlaying(PlaybackStatus.FINISHED);

        assertThatThrownBy(() -> skipVoteService.vote(PLAYBACK_ID, USER_ID))
                .isInstanceOf(PlaybackNotPlayingException.class);
    }

    @Test
    void throwsWhenPlaybackSkipped() {
        stubNotPlaying(PlaybackStatus.SKIPPED);

        assertThatThrownBy(() -> skipVoteService.vote(PLAYBACK_ID, USER_ID))
                .isInstanceOf(PlaybackNotPlayingException.class);
    }

    @Test
    void throwsWhenPlaybackError() {
        stubNotPlaying(PlaybackStatus.ERROR);

        assertThatThrownBy(() -> skipVoteService.vote(PLAYBACK_ID, USER_ID))
                .isInstanceOf(PlaybackNotPlayingException.class);
    }

    @Test
    void throwsWhenUserNotInRoom() {
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(playback.getStatus()).thenReturn(PlaybackStatus.PLAYING);
        when(playback.getQueueItem()).thenReturn(queueItem);
        when(queueItem.getRoom()).thenReturn(room);
        when(room.getId()).thenReturn(ROOM_ID);
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> skipVoteService.vote(PLAYBACK_ID, USER_ID))
                .isInstanceOf(UserNotInRoomException.class);

        verify(skipVoteRepository, never()).save(any(SkipVote.class));
    }

    @Test
    void throwsWhenUserAlreadyVoted() {
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(playback.getStatus()).thenReturn(PlaybackStatus.PLAYING);
        when(playback.getQueueItem()).thenReturn(queueItem);
        when(queueItem.getRoom()).thenReturn(room);
        when(room.getId()).thenReturn(ROOM_ID);
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(skipVoteRepository.existsByPlaybackIdAndUserId(PLAYBACK_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> skipVoteService.vote(PLAYBACK_ID, USER_ID))
                .isInstanceOf(SkipVoteAlreadyExistsException.class);

        verify(skipVoteRepository, never()).save(any(SkipVote.class));
    }

    @Test
    void voteIsTransactional() throws NoSuchMethodException {
        Method method = SkipVoteService.class.getMethod("vote", Long.class, Long.class);

        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private void stubNotPlaying(PlaybackStatus status) {
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(playback.getStatus()).thenReturn(status);
    }

    private void stubHappyPath() {
        when(playbackRepository.findById(PLAYBACK_ID)).thenReturn(Optional.of(playback));
        when(playback.getStatus()).thenReturn(PlaybackStatus.PLAYING);
        when(playback.getQueueItem()).thenReturn(queueItem);
        when(queueItem.getRoom()).thenReturn(room);
        when(room.getId()).thenReturn(ROOM_ID);
        when(roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, USER_ID)).thenReturn(true);
        when(skipVoteRepository.existsByPlaybackIdAndUserId(PLAYBACK_ID, USER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(skipVoteRepository.save(any(SkipVote.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}