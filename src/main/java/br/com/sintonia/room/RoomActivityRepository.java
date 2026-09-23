package br.com.sintonia.room;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomActivityRepository extends JpaRepository<RoomActivity, Long> {

    List<RoomActivity> findByRoomIdOrderByCreatedAtDescIdDesc(Long roomId, Pageable pageable);
}
