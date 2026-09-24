package br.com.sintonia.room;

import java.util.List;

public record RoomSummaryResponse(
        Long roomId,
        long playbackCount,
        long participantCount,
        long skippedCount,
        long autoDjPlaybackCount,
        long skipVoteCount,
        List<UserContribution> contributions
) {

    public record UserContribution(
            Long userId,
            String displayName,
            String avatarUrl,
            long addedCount,
            long playedCount,
            long skippedCount,
            long skipVoteCount
    ) {
    }
}
