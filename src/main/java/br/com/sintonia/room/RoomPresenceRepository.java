package br.com.sintonia.room;

import br.com.sintonia.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RoomPresenceRepository extends JpaRepository<RoomPresence, Long> {

    Optional<RoomPresence> findByRoomAndUserAndClientSessionId(Room room, User user, String clientSessionId);

    boolean existsByRoomIdAndUserIdAndExpiresAtAfter(Long roomId, Long userId, Instant now);

    boolean existsByRoomIdAndUserIdAndClientSessionIdAndExpiresAtAfter(
            Long roomId, Long userId, String clientSessionId, Instant now);

    @Query("SELECT COUNT(DISTINCT p.user.id) FROM RoomPresence p "
            + "WHERE p.room = :room AND p.expiresAt > :now")
    long countPresentUsers(@Param("room") Room room, @Param("now") Instant now);

    @Query("SELECT p.id, p.room.id FROM RoomPresence p WHERE p.expiresAt <= :now")
    List<Object[]> findExpiredCandidates(@Param("now") Instant now);
}
