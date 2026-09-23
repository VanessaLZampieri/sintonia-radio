package br.com.sintonia.room;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RoomActivityRepositoryTest {

    @Autowired
    private RoomActivityRepository roomActivityRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsSnapshotFields() {
        Room room = persistRoom("RAC00001");

        RoomActivity activity = new RoomActivity(room, RoomActivityType.SONG_ADDED,
                10L, "Vanessa", 1L, "Everlong", null);
        entityManager.persist(activity);
        entityManager.flush();
        entityManager.clear();

        RoomActivity found = entityManager.find(RoomActivity.class, activity.getId());

        assertThat(found.getActorDisplayName()).isEqualTo("Vanessa");
        assertThat(found.getSongTitle()).isEqualTo("Everlong");
        assertThat(found.getType()).isEqualTo(RoomActivityType.SONG_ADDED);
    }

    @Test
    void ordersMostRecentFirst() {
        Room room = persistRoom("RAC00002");
        persistActivity(room, "um");
        persistActivity(room, "dois");
        persistActivity(room, "tres");
        entityManager.flush();

        List<RoomActivity> result = roomActivityRepository
                .findByRoomIdOrderByCreatedAtDescIdDesc(room.getId(), PageRequest.of(0, 50));

        assertThat(result).extracting(RoomActivity::getDetail)
                .containsExactly("tres", "dois", "um");
    }

    @Test
    void limitsToFifty() {
        Room room = persistRoom("RAC00003");
        for (int i = 0; i < 55; i++) {
            persistActivity(room, "d" + i);
        }
        entityManager.flush();

        List<RoomActivity> result = roomActivityRepository
                .findByRoomIdOrderByCreatedAtDescIdDesc(room.getId(), PageRequest.of(0, 50));

        assertThat(result).hasSize(50);
    }

    private void persistActivity(Room room, String detail) {
        entityManager.persist(new RoomActivity(room, RoomActivityType.ROOM_RENAMED,
                null, null, null, null, detail));
    }

    private Room persistRoom(String code) {
        Room room = new Room("Sala " + code, code, RoomStatus.ACTIVE);
        entityManager.persist(room);
        return room;
    }
}
