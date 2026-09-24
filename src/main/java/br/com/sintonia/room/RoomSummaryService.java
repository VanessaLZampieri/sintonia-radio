package br.com.sintonia.room;

import br.com.sintonia.playback.PlaybackRepository;
import br.com.sintonia.playback.PlaybackStatus;
import br.com.sintonia.playback.SkipVoteRepository;
import br.com.sintonia.queue.QueueItemSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomSummaryService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomActivityRepository roomActivityRepository;
    private final PlaybackRepository playbackRepository;
    private final SkipVoteRepository skipVoteRepository;

    public RoomSummaryService(RoomRepository roomRepository,
                              RoomMemberRepository roomMemberRepository,
                              RoomActivityRepository roomActivityRepository,
                              PlaybackRepository playbackRepository,
                              SkipVoteRepository skipVoteRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomActivityRepository = roomActivityRepository;
        this.playbackRepository = playbackRepository;
        this.skipVoteRepository = skipVoteRepository;
    }

    @Transactional(readOnly = true)
    public RoomSummaryResponse get(Long roomId, Long userId) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new UserNotInRoomException("Usuário não participou desta sala.");
        }

        PlaybackRepository.RoomPlaybackSummary playback = playbackRepository.summarizeRoom(
                roomId, PlaybackStatus.SKIPPED, QueueItemSource.AUTO_DJ);
        Map<Long, RoomActivityRepository.UserAdditionSummary> additions = roomActivityRepository
                .summarizeAdditionsByUser(roomId, RoomActivityType.SONG_ADDED).stream()
                .collect(Collectors.toMap(RoomActivityRepository.UserAdditionSummary::getUserId, Function.identity()));
        Map<Long, PlaybackRepository.UserPlaybackSummary> userPlaybacks = playbackRepository
                .summarizeUserContributions(roomId, PlaybackStatus.SKIPPED, QueueItemSource.USER).stream()
                .collect(Collectors.toMap(PlaybackRepository.UserPlaybackSummary::getUserId, Function.identity()));
        Map<Long, SkipVoteRepository.UserVoteSummary> votes = skipVoteRepository
                .summarizeVotesByUser(roomId).stream()
                .collect(Collectors.toMap(SkipVoteRepository.UserVoteSummary::getUserId, Function.identity()));

        var contributions = roomMemberRepository.findSummaryParticipants(roomId).stream()
                .map(participant -> contribution(participant, additions, userPlaybacks, votes))
                .toList();
        long totalVotes = votes.values().stream().mapToLong(SkipVoteRepository.UserVoteSummary::getVoteCount).sum();

        return new RoomSummaryResponse(
                roomId,
                value(playback.getPlaybackCount()),
                roomMemberRepository.countDistinctHistoricalUsersByRoomId(roomId),
                value(playback.getSkippedCount()),
                value(playback.getAutoDjCount()),
                totalVotes,
                contributions);
    }

    private RoomSummaryResponse.UserContribution contribution(
            RoomMemberRepository.SummaryParticipant participant,
            Map<Long, RoomActivityRepository.UserAdditionSummary> additions,
            Map<Long, PlaybackRepository.UserPlaybackSummary> playbacks,
            Map<Long, SkipVoteRepository.UserVoteSummary> votes) {
        Long userId = participant.getUserId();
        RoomActivityRepository.UserAdditionSummary addition = additions.get(userId);
        PlaybackRepository.UserPlaybackSummary playback = playbacks.get(userId);
        SkipVoteRepository.UserVoteSummary vote = votes.get(userId);
        return new RoomSummaryResponse.UserContribution(
                userId,
                participant.getDisplayName(),
                participant.getAvatarUrl(),
                addition == null ? 0 : value(addition.getAddedCount()),
                playback == null ? 0 : value(playback.getPlayedCount()),
                playback == null ? 0 : value(playback.getSkippedCount()),
                vote == null ? 0 : value(vote.getVoteCount()));
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }
}
