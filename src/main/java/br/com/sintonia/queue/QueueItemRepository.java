package br.com.sintonia.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {

    boolean existsByRoomIdAndSongIdAndStatusIn(Long roomId, Long songId, Collection<QueueItemStatus> statuses);

    long countByRoomIdAndUserIdAndStatus(Long roomId, Long userId, QueueItemStatus status);

    long countByRoomIdAndStatus(Long roomId, QueueItemStatus status);

    @Query("SELECT q.user.id, COUNT(q) FROM QueueItem q "
            + "WHERE q.room.id = :roomId AND q.status = :status AND q.user IS NOT NULL "
            + "GROUP BY q.user.id")
    List<Object[]> countWaitingByUser(@Param("roomId") Long roomId, @Param("status") QueueItemStatus status);

    @Query("SELECT COALESCE(MAX(q.position), 0) FROM QueueItem q WHERE q.room.id = :roomId")
    Integer findMaxPositionByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT q FROM QueueItem q JOIN FETCH q.song LEFT JOIN FETCH q.user "
            + "WHERE q.room.id = :roomId AND q.status = :status ORDER BY q.position ASC, q.id ASC")
    List<QueueItem> findAllByRoomIdAndStatusOrderByPositionAscIdAsc(
            @Param("roomId") Long roomId, @Param("status") QueueItemStatus status);

    Optional<QueueItem> findByIdAndRoomId(Long id, Long roomId);

    Optional<QueueItem> findByRoomIdAndStatus(Long roomId, QueueItemStatus status);

    Optional<QueueItem> findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(Long roomId, QueueItemStatus status);
}
