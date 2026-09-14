package br.com.sintonia.playback;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaybackRepository extends JpaRepository<Playback, Long> {

    Optional<Playback> findByQueueItemRoomIdAndStatus(Long roomId, PlaybackStatus status);

    List<Playback> findByQueueItemId(Long queueItemId);

    List<Playback> findByQueueItemRoomIdAndStatusInOrderByStartedAtDesc(Long roomId, List<PlaybackStatus> statuses, Pageable pageable);
}