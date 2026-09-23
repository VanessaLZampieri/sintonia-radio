package br.com.sintonia.room;

import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomActivityService {

    private static final int MAX_ACTIVITIES = 50;

    private final RoomActivityRepository roomActivityRepository;
    private final RoomRepository roomRepository;

    public RoomActivityService(RoomActivityRepository roomActivityRepository,
                               RoomRepository roomRepository) {
        this.roomActivityRepository = roomActivityRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public void record(Room room, RoomActivityType type, User actor, Song song, String detail) {
        Long actorUserId = actor == null ? null : actor.getId();
        String actorDisplayName = actor == null ? null : actor.getDisplayName();
        Long songId = song == null ? null : song.getId();
        String songTitle = song == null ? null : song.getTitle();
        roomActivityRepository.save(
                new RoomActivity(room, type, actorUserId, actorDisplayName, songId, songTitle, detail));
    }

    @Transactional(readOnly = true)
    public List<RoomActivityResponse> findRecent(Long roomId) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        return roomActivityRepository.findByRoomIdOrderByCreatedAtDescIdDesc(
                        roomId, PageRequest.of(0, MAX_ACTIVITIES)).stream()
                .map(RoomActivityResponse::from)
                .toList();
    }
}
