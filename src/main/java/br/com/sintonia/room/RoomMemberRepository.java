package br.com.sintonia.room;

import br.com.sintonia.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    Optional<RoomMember> findByRoomAndUserAndLeftAtIsNull(Room room, User user);

    long countByRoomAndLeftAtIsNull(Room room);

    boolean existsByRoomIdAndUserIdAndLeftAtIsNull(Long roomId, Long userId);

    List<RoomMember> findAllByRoomIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(Long roomId);

    List<RoomMember> findByUserIdOrderByJoinedAtDesc(Long userId);
}
