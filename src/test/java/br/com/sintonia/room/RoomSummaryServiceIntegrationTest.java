package br.com.sintonia.room;

import br.com.sintonia.playback.Playback;
import br.com.sintonia.playback.PlaybackStatus;
import br.com.sintonia.playback.SkipVote;
import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RoomSummaryServiceIntegrationTest {

    @Autowired
    private RoomSummaryService roomSummaryService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void summarizesHistoricalRoomWithoutDoubleCountingContributions() {
        Room room = new Room("Resumo", "SUMM0001", RoomStatus.ACTIVE);
        entityManager.persist(room);
        User ana = persistUser("summary-ana", "Ana", "summary-ana@example.com", null);
        User bruno = persistUser("summary-bruno", "Bruno", "summary-bruno@example.com", "https://img/bruno.jpg");

        RoomMember anaFirstVisit = new RoomMember(room, ana);
        anaFirstVisit.leave();
        RoomMember anaSecondVisit = new RoomMember(room, ana);
        anaSecondVisit.leave();
        RoomMember brunoVisit = new RoomMember(room, bruno);
        brunoVisit.leave();
        entityManager.persist(anaFirstVisit);
        entityManager.persist(anaSecondVisit);
        entityManager.persist(brunoVisit);

        Song first = persistSong("summary-1", "Primeira");
        Song second = persistSong("summary-2", "Segunda");
        Song waiting = persistSong("summary-3", "Aguardando");
        Song autoDj = persistSong("summary-4", "Auto DJ");
        Song removed = persistSong("summary-5", "Removida");

        QueueItem anaFinished = persistUserItem(room, first, ana, 1, QueueItemStatus.FINISHED);
        QueueItem anaSkipped = persistUserItem(room, second, ana, 2, QueueItemStatus.SKIPPED);
        persistUserItem(room, waiting, bruno, 3, QueueItemStatus.WAITING);
        QueueItem autoDjFinished = new QueueItem(room, autoDj, Instant.now(), 4);
        autoDjFinished.setStatus(QueueItemStatus.FINISHED);
        entityManager.persist(autoDjFinished);

        Playback firstPlayback = persistPlayback(anaFinished, PlaybackStatus.FINISHED);
        persistPlayback(anaFinished, PlaybackStatus.FINISHED);
        Playback skippedPlayback = persistPlayback(anaSkipped, PlaybackStatus.SKIPPED);
        persistPlayback(autoDjFinished, PlaybackStatus.FINISHED);

        entityManager.persist(new SkipVote(firstPlayback, ana, Instant.now()));
        entityManager.persist(new SkipVote(skippedPlayback, ana, Instant.now()));
        entityManager.persist(new SkipVote(skippedPlayback, bruno, Instant.now()));

        persistAddedActivity(room, ana, first);
        persistAddedActivity(room, ana, second);
        persistAddedActivity(room, ana, removed);
        persistAddedActivity(room, bruno, waiting);

        room.close();
        entityManager.flush();
        entityManager.clear();

        RoomSummaryResponse result = roomSummaryService.get(room.getId(), ana.getId());

        assertThat(result.playbackCount()).isEqualTo(4);
        assertThat(result.participantCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.autoDjPlaybackCount()).isEqualTo(1);
        assertThat(result.skipVoteCount()).isEqualTo(3);
        assertThat(result.contributions()).hasSize(2);
        assertThat(result.contributions()).extracting(RoomSummaryResponse.UserContribution::displayName)
                .containsExactly("Ana", "Bruno");
        RoomSummaryResponse.UserContribution anaResult = result.contributions().get(0);
        assertThat(anaResult.addedCount()).isEqualTo(3);
        assertThat(anaResult.playedCount()).isEqualTo(2);
        assertThat(anaResult.skippedCount()).isEqualTo(1);
        assertThat(anaResult.skipVoteCount()).isEqualTo(2);
        RoomSummaryResponse.UserContribution brunoResult = result.contributions().get(1);
        assertThat(brunoResult.addedCount()).isEqualTo(1);
        assertThat(brunoResult.playedCount()).isZero();
        assertThat(brunoResult.skippedCount()).isZero();
        assertThat(brunoResult.skipVoteCount()).isEqualTo(1);
        assertThat(result.contributions()).noneMatch(contribution -> contribution.displayName().equals("Auto-DJ"));
    }

    private User persistUser(String googleId, String name, String email, String avatarUrl) {
        User user = new User(googleId, name, email, avatarUrl);
        entityManager.persist(user);
        return user;
    }

    private Song persistSong(String videoId, String title) {
        Song song = new Song(videoId, title, null, Duration.ofMinutes(3));
        entityManager.persist(song);
        return song;
    }

    private QueueItem persistUserItem(Room room, Song song, User user, int position, QueueItemStatus status) {
        QueueItem item = new QueueItem(room, song, user, Instant.now(), position);
        item.setStatus(status);
        entityManager.persist(item);
        return item;
    }

    private Playback persistPlayback(QueueItem item, PlaybackStatus status) {
        Playback playback = new Playback(item, Instant.now().minusSeconds(30));
        playback.setStatus(status);
        playback.setEndedAt(Instant.now());
        entityManager.persist(playback);
        return playback;
    }

    private void persistAddedActivity(Room room, User user, Song song) {
        entityManager.persist(new RoomActivity(
                room, RoomActivityType.SONG_ADDED, user.getId(), user.getDisplayName(),
                song.getId(), song.getTitle(), null));
    }
}
