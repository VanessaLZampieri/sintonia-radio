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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RoomMemberRepositoryTest {

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findsOnlyPresentMembers() {
        Room room = new Room("Sala Teste", "RM000001", RoomStatus.ACTIVE);
        entityManager.persist(room);

        User ana = persistUser("rm-g1", "Ana", "rm1@example.com");
        User bruno = persistUser("rm-g2", "Bruno", "rm2@example.com");
        User carla = persistUser("rm-g3", "Carla", "rm3@example.com");

        RoomMember present1 = new RoomMember(room, ana);
        RoomMember present2 = new RoomMember(room, bruno);
        RoomMember left = new RoomMember(room, carla);
        left.leave();
        entityManager.persist(present1);
        entityManager.persist(present2);
        entityManager.persist(left);
        entityManager.flush();

        List<RoomMember> result = roomMemberRepository
                .findAllByRoomIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(room.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(m -> m.getUser().getId())
                .contains(ana.getId(), bruno.getId());
    }

    @Test
    void findByUserIdReturnsOnlyThatUsersMemberships() {
        Room roomA = new Room("Sala A", "RM000010", RoomStatus.ACTIVE);
        Room roomB = new Room("Sala B", "RM000011", RoomStatus.ACTIVE);
        entityManager.persist(roomA);
        entityManager.persist(roomB);

        User ana = persistUser("rm-g10", "Ana", "rm10@example.com");
        User bruno = persistUser("rm-g11", "Bruno", "rm11@example.com");

        entityManager.persist(new RoomMember(roomA, ana));
        entityManager.persist(new RoomMember(roomB, bruno));
        entityManager.flush();

        List<RoomMember> result = roomMemberRepository.findByUserIdOrderByJoinedAtDesc(ana.getId());

        assertThat(result).hasSize(1);
        assertThat(result).extracting(m -> m.getRoom().getId()).containsExactly(roomA.getId());
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
