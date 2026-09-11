package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomMemberRepository;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SkipVoteService {

    private static final int SKIP_THRESHOLD_PERCENT = 60;

    private final PlaybackRepository playbackRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SkipVoteRepository skipVoteRepository;
    private final UserRepository userRepository;
    private final PlaybackService playbackService;

    public SkipVoteService(PlaybackRepository playbackRepository,
                           RoomMemberRepository roomMemberRepository,
                           SkipVoteRepository skipVoteRepository,
                           UserRepository userRepository,
                           PlaybackService playbackService) {
        this.playbackRepository = playbackRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.skipVoteRepository = skipVoteRepository;
        this.userRepository = userRepository;
        this.playbackService = playbackService;
    }

    @Transactional
    public SkipVote vote(Long playbackId, Long userId) {
        Playback playback = playbackRepository.findById(playbackId)
                .orElseThrow(() -> new PlaybackNotFoundException("Playback não encontrado."));

        if (playback.getStatus() != PlaybackStatus.PLAYING) {
            throw new PlaybackNotPlayingException("O playback não está em andamento.");
        }

        QueueItem queueItem = playback.getQueueItem();
        Room room = queueItem.getRoom();

        if (!roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId)) {
            throw new UserNotInRoomException("Usuário não está na sala.");
        }

        if (skipVoteRepository.existsByPlaybackIdAndUserId(playbackId, userId)) {
            throw new SkipVoteAlreadyExistsException("O usuário já votou neste playback.");
        }

        User user = userRepository.getReferenceById(userId);

        SkipVote vote = new SkipVote(playback, user, Instant.now());
        SkipVote saved = skipVoteRepository.save(vote);

        if (hasReachedSkipThreshold(playbackId)) {
            playbackService.skip(playbackId);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public boolean hasReachedSkipThreshold(Long playbackId) {
        Playback playback = playbackRepository.findById(playbackId)
                .orElseThrow(() -> new PlaybackNotFoundException("Playback não encontrado."));

        Room room = playback.getQueueItem().getRoom();

        long participants = roomMemberRepository.countByRoomAndLeftAtIsNull(room);
        long votes = skipVoteRepository.countByPlaybackId(playbackId);

        long requiredVotes = Math.ceilDiv(participants * SKIP_THRESHOLD_PERCENT, 100);

        return votes >= requiredVotes;
    }
}