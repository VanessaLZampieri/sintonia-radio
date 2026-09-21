package br.com.sintonia.playback;

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

    List<Playback> findByQueueItemId(Long queueItemId);

    List<Playback> findByQueueItemRoomIdAndStatusInOrderByStartedAtDesc(Long roomId, List<PlaybackStatus> statuses, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Playback p WHERE p.id = :id")
    Optional<Playback> findByIdForUpdate(@Param("id") Long id);
}
