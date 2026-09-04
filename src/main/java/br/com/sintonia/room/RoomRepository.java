package br.com.sintonia.room;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.code = :code")
    Optional<Room> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r.id FROM Room r WHERE r.status = :status AND r.emptySince IS NOT NULL AND r.emptySince <= :threshold")
    List<Long> findExpiredCandidateIds(@Param("status") RoomStatus status, @Param("threshold") Instant threshold);
}
