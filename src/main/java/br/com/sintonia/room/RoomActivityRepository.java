package br.com.sintonia.room;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomActivityRepository extends JpaRepository<RoomActivity, Long> {

    List<RoomActivity> findByRoomIdOrderByCreatedAtDescIdDesc(Long roomId, Pageable pageable);

    @Query("SELECT a.actorUserId AS userId, COUNT(a) AS addedCount FROM RoomActivity a "
            + "WHERE a.room.id = :roomId AND a.type = :type AND a.actorUserId IS NOT NULL "
            + "GROUP BY a.actorUserId")
    List<UserAdditionSummary> summarizeAdditionsByUser(
            @Param("roomId") Long roomId, @Param("type") RoomActivityType type);

    interface UserAdditionSummary {
        Long getUserId();

        Long getAddedCount();
    }
}
