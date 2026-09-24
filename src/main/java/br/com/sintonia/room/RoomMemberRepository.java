package br.com.sintonia.room;

import br.com.sintonia.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    Optional<RoomMember> findByRoomAndUserAndLeftAtIsNull(Room room, User user);

    @Query("SELECT COUNT(DISTINCT m.user.id) FROM RoomMember m WHERE m.room = :room AND m.leftAt IS NULL "
            + "AND EXISTS (SELECT p FROM RoomPresence p WHERE p.member = m AND p.expiresAt > CURRENT_TIMESTAMP)")
    long countByRoomAndLeftAtIsNull(@Param("room") Room room);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM RoomMember m "
            + "WHERE m.room.id = :roomId AND m.user.id = :userId AND m.leftAt IS NULL "
            + "AND EXISTS (SELECT p FROM RoomPresence p WHERE p.member = m AND p.expiresAt > CURRENT_TIMESTAMP)")
    boolean existsByRoomIdAndUserIdAndLeftAtIsNull(@Param("roomId") Long roomId,
                                                    @Param("userId") Long userId);

    @Query("SELECT m FROM RoomMember m WHERE m.room.id = :roomId AND m.leftAt IS NULL "
            + "AND EXISTS (SELECT p FROM RoomPresence p WHERE p.member = m AND p.expiresAt > CURRENT_TIMESTAMP) "
            + "ORDER BY m.joinedAt ASC, m.id ASC")
    List<RoomMember> findAllByRoomIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(@Param("roomId") Long roomId);

    @Query("SELECT m.id FROM RoomMember m WHERE m.leftAt IS NULL "
            + "AND NOT EXISTS (SELECT p FROM RoomPresence p WHERE p.member = m)")
    List<Long> findOpenMemberIdsWithoutPresence();

    List<RoomMember> findByUserIdOrderByJoinedAtDesc(Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    @Query("SELECT m FROM RoomMember m JOIN FETCH m.room r "
            + "WHERE m.user.id = :userId AND r.status = :status ORDER BY m.joinedAt DESC")
    List<RoomMember> findByUserIdAndRoomStatusOrderByJoinedAtDesc(
            @Param("userId") Long userId, @Param("status") RoomStatus status);

    @Query("SELECT COUNT(DISTINCT m.user.id) FROM RoomMember m WHERE m.room.id = :roomId")
    long countDistinctHistoricalUsersByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT u.id AS userId, u.displayName AS displayName, u.avatarUrl AS avatarUrl "
            + "FROM RoomMember m JOIN m.user u WHERE m.room.id = :roomId "
            + "GROUP BY u.id, u.displayName, u.avatarUrl ORDER BY u.displayName, u.id")
    List<SummaryParticipant> findSummaryParticipants(@Param("roomId") Long roomId);

    interface SummaryParticipant {
        Long getUserId();

        String getDisplayName();

        String getAvatarUrl();
    }
}
