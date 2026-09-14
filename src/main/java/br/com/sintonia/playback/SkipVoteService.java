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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class SkipVoteService {

    private static final int SKIP_THRESHOLD_PERCENT = 60;

    private final RoomRepository roomRepository;
    private final PlaybackRepository playbackRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SkipVoteRepository skipVoteRepository;
    private final UserRepository userRepository;
    private final PlaybackService playbackService;
    private final RoomEventPublisher roomEventPublisher;

    public SkipVoteService(RoomRepository roomRepository,
                           PlaybackRepository playbackRepository,
                           RoomMemberRepository roomMemberRepository,
                           SkipVoteRepository skipVoteRepository,
                           UserRepository userRepository,
                           PlaybackService playbackService,
                           RoomEventPublisher roomEventPublisher) {
        this.roomRepository = roomRepository;
        this.playbackRepository = playbackRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.skipVoteRepository = skipVoteRepository;
        this.userRepository = userRepository;
        this.playbackService = playbackService;
        this.roomEventPublisher = roomEventPublisher;
    }

    @Transactional
    public SkipVoteResponse vote(Long roomId, Long userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        if (!roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)) {
            throw new UserNotInRoomException("Usuário não está na sala.");
        }

        Playback playback = playbackRepository.findByQueueItemRoomIdAndStatus(roomId, PlaybackStatus.PLAYING)
                .orElseThrow(() -> new PlaybackNotFoundException("Não há playback em andamento na sala."));

        Long playbackId = playback.getId();

        if (skipVoteRepository.existsByPlaybackIdAndUserId(playbackId, userId)) {
            throw new SkipVoteAlreadyExistsException("O usuário já votou neste playback.");
        }

        User user = userRepository.getReferenceById(userId);
        skipVoteRepository.save(new SkipVote(playback, user, Instant.now()));

        long participants = roomMemberRepository.countByRoomAndLeftAtIsNull(room);
        long votes = skipVoteRepository.countByPlaybackId(playbackId);
        long requiredVotes = requiredVotes(participants);
        boolean skipped = votes >= requiredVotes;

        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.SKIP_VOTE_CHANGED, roomId,
                        new SkipVoteChangedEventPayload(playbackId, votes, requiredVotes)));

        if (skipped) {
            playbackService.skip(playbackId);
        }

        return new SkipVoteResponse(playbackId, votes, requiredVotes, skipped);
    }

    @Transactional(readOnly = true)
    public Optional<SkipVoteResponse> current(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return playbackRepository.findByQueueItemRoomIdAndStatus(roomId, PlaybackStatus.PLAYING)
                .map(playback -> {
                    long participants = roomMemberRepository.countByRoomAndLeftAtIsNull(room);
                    long votes = skipVoteRepository.countByPlaybackId(playback.getId());
                    long requiredVotes = requiredVotes(participants);
                    return new SkipVoteResponse(playback.getId(), votes, requiredVotes, votes >= requiredVotes);
                });
    }

    public static long requiredVotes(long participants) {
        return Math.ceilDiv(participants * SKIP_THRESHOLD_PERCENT, 100);
    }
}
