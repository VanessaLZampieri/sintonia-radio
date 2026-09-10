package br.com.sintonia.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {

    boolean existsByRoomIdAndSongId(Long roomId, Long songId);

    long countByRoomIdAndUserId(Long roomId, Long userId);

    @Query("SELECT COALESCE(MAX(q.position), 0) FROM QueueItem q WHERE q.room.id = :roomId")
    Integer findMaxPositionByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT q FROM QueueItem q JOIN FETCH q.song JOIN FETCH q.user WHERE q.room.id = :roomId ORDER BY q.position ASC, q.id ASC")
    List<QueueItem> findAllByRoomIdOrderByPositionAscIdAsc(@Param("roomId") Long roomId);

    Optional<QueueItem> findByIdAndRoomId(Long id, Long roomId);

    Optional<QueueItem> findByRoomIdAndStatus(Long roomId, QueueItemStatus status);
}
