package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItemSource;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaybackRepository extends JpaRepository<Playback, Long> {

    Optional<Playback> findByQueueItemRoomIdAndStatus(Long roomId, PlaybackStatus status);

    Optional<Playback> findFirstByQueueItemRoomIdAndStatusOrderByStartedAtDesc(Long roomId, PlaybackStatus status);

    List<Playback> findByQueueItemId(Long queueItemId);

    List<Playback> findByQueueItemRoomIdAndStatusInOrderByStartedAtDesc(Long roomId, List<PlaybackStatus> statuses, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Playback p WHERE p.id = :id")
    Optional<Playback> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(p) AS playbackCount, "
            + "COALESCE(SUM(CASE WHEN p.status = :skippedStatus THEN 1 ELSE 0 END), 0) AS skippedCount, "
            + "COALESCE(SUM(CASE WHEN p.queueItem.source = :autoDjSource THEN 1 ELSE 0 END), 0) AS autoDjCount "
            + "FROM Playback p WHERE p.queueItem.room.id = :roomId")
    RoomPlaybackSummary summarizeRoom(@Param("roomId") Long roomId,
                                      @Param("skippedStatus") PlaybackStatus skippedStatus,
                                      @Param("autoDjSource") QueueItemSource autoDjSource);

    @Query("SELECT q.user.id AS userId, COUNT(DISTINCT q.id) AS playedCount, "
            + "COUNT(DISTINCT CASE WHEN p.status = :skippedStatus THEN q.id ELSE NULL END) AS skippedCount "
            + "FROM Playback p JOIN p.queueItem q "
            + "WHERE q.room.id = :roomId AND q.source = :userSource AND q.user IS NOT NULL "
            + "GROUP BY q.user.id")
    List<UserPlaybackSummary> summarizeUserContributions(
            @Param("roomId") Long roomId,
            @Param("skippedStatus") PlaybackStatus skippedStatus,
            @Param("userSource") QueueItemSource userSource);

    interface RoomPlaybackSummary {
        Long getPlaybackCount();

        Long getSkippedCount();

        Long getAutoDjCount();
    }

    interface UserPlaybackSummary {
        Long getUserId();

        Long getPlayedCount();

        Long getSkippedCount();
    }
}
