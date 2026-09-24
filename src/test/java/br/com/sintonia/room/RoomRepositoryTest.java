package br.com.sintonia.room;

import br.com.sintonia.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findsActiveRoomsWithPresentMembersOnly() {
        Room activeWithMember = persistRoom("RRA00001", RoomStatus.ACTIVE);
        Room activeEmpty = persistRoom("RRA00002", RoomStatus.ACTIVE);
        Room closedWithMember = persistRoom("RRA00003", RoomStatus.CLOSED);

        User user = persistUser("rr-g1", "Ana", "rr1@example.com");
        RoomMember activeMember = new RoomMember(activeWithMember, user);
        RoomMember closedMember = new RoomMember(closedWithMember, user);
        entityManager.persist(activeMember);
        entityManager.persist(closedMember);
        entityManager.persist(new RoomPresence(activeWithMember, user, activeMember,
                "550e8400-e29b-41d4-a716-446655440000", Instant.now().plusSeconds(90)));
        entityManager.persist(new RoomPresence(closedWithMember, user, closedMember,
                "550e8400-e29b-41d4-a716-446655440001", Instant.now().plusSeconds(90)));
        entityManager.flush();

        List<Room> result = roomRepository.findActiveRoomsWithPresentMembers(RoomStatus.ACTIVE);

        assertThat(result).extracting(Room::getId)
                .contains(activeWithMember.getId())
                .doesNotContain(activeEmpty.getId(), closedWithMember.getId());
    }

    @Test
    void excludesActiveRoomInGracePeriodWithNoPresentMembers() {
        Room activeEmptied = persistRoom("RRA00004", RoomStatus.ACTIVE);
        User user = persistUser("rr-g2", "Bruno", "rr2@example.com");
        RoomMember left = new RoomMember(activeEmptied, user);
        left.leave();
        entityManager.persist(left);
        entityManager.flush();

        List<Room> result = roomRepository.findActiveRoomsWithPresentMembers(RoomStatus.ACTIVE);

        assertThat(result).extracting(Room::getId).doesNotContain(activeEmptied.getId());
    }

    @Test
    void excludesLegacyOpenMemberWithoutLivePresence() {
        Room ghostRoom = persistRoom("RRA00005", RoomStatus.ACTIVE);
        User user = persistUser("rr-g3", "Carla", "rr3@example.com");
        entityManager.persist(new RoomMember(ghostRoom, user));
        entityManager.flush();

        List<Room> result = roomRepository.findActiveRoomsWithPresentMembers(RoomStatus.ACTIVE);

        assertThat(result).extracting(Room::getId).doesNotContain(ghostRoom.getId());
    }

    private Room persistRoom(String code, RoomStatus status) {
        Room room = new Room("Sala " + code, code, status);
        entityManager.persist(room);
        return room;
    }

    private User persistUser(String googleId, String displayName, String email) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            ReflectionTestUtils.setField(user, "googleId", googleId);
            ReflectionTestUtils.setField(user, "name", displayName + " Completo");
            ReflectionTestUtils.setField(user, "displayName", displayName);
            ReflectionTestUtils.setField(user, "email", email);
            entityManager.persist(user);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível criar User de teste", e);
        }
    }
}
