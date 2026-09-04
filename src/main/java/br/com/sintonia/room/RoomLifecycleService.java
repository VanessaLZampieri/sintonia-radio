package br.com.sintonia.room;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class RoomLifecycleService {

    private static final Duration GRACE_PERIOD = Duration.ofMinutes(20);

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;

    public RoomLifecycleService(RoomRepository roomRepository, RoomMemberRepository roomMemberRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    @Transactional
    public void closeExpiredRooms() {
        Instant threshold = Instant.now().minus(GRACE_PERIOD);

        for (Long roomId : roomRepository.findExpiredCandidateIds(RoomStatus.ACTIVE, threshold)) {
            Room room = roomRepository.findByIdForUpdate(roomId).orElse(null);
            if (room == null || room.getStatus() != RoomStatus.ACTIVE) {
                continue;
            }
            if (room.getEmptySince() == null || room.getEmptySince().isAfter(threshold)) {
                continue;
            }
            if (roomMemberRepository.countByRoomAndLeftAtIsNull(room) > 0) {
                continue;
            }
            room.close();
        }
    }
}
