package br.com.sintonia.playback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkipVoteRepository extends JpaRepository<SkipVote, Long> {

    boolean existsByPlaybackIdAndUserId(Long playbackId, Long userId);

    long countByPlaybackId(Long playbackId);

    @Query("SELECT v.user.id AS userId, COUNT(v) AS voteCount FROM SkipVote v "
            + "WHERE v.playback.queueItem.room.id = :roomId GROUP BY v.user.id")
    List<UserVoteSummary> summarizeVotesByUser(@Param("roomId") Long roomId);

    interface UserVoteSummary {
        Long getUserId();

        Long getVoteCount();
    }
}
