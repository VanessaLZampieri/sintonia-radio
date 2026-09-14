package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.song.Song;
import br.com.sintonia.song.SongRepository;
import br.com.sintonia.websocket.QueueChangeAction;
import br.com.sintonia.websocket.QueueChangedEventPayload;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import br.com.sintonia.websocket.RoomEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
public class AutoDjService {

    private static final int RECENT_WINDOW = 10;

    private final RoomRepository roomRepository;
    private final SongRepository songRepository;
    private final QueueItemRepository queueItemRepository;
    private final PlaybackRepository playbackRepository;
    private final PlaybackHistoryService playbackHistoryService;
    private final RoomEventPublisher roomEventPublisher;
    private final Random random;

    @Autowired
    public AutoDjService(RoomRepository roomRepository,
                         SongRepository songRepository,
                         QueueItemRepository queueItemRepository,
                         PlaybackRepository playbackRepository,
                         PlaybackHistoryService playbackHistoryService,
                         RoomEventPublisher roomEventPublisher) {
        this(roomRepository, songRepository, queueItemRepository, playbackRepository,
                playbackHistoryService, roomEventPublisher, new Random());
    }

    AutoDjService(RoomRepository roomRepository,
                  SongRepository songRepository,
                  QueueItemRepository queueItemRepository,
                  PlaybackRepository playbackRepository,
                  PlaybackHistoryService playbackHistoryService,
                  RoomEventPublisher roomEventPublisher,
                  Random random) {
        this.roomRepository = roomRepository;
        this.songRepository = songRepository;
        this.queueItemRepository = queueItemRepository;
        this.playbackRepository = playbackRepository;
        this.playbackHistoryService = playbackHistoryService;
        this.roomEventPublisher = roomEventPublisher;
        this.random = random;
    }

    @Transactional
    public Optional<QueueItem> createNext(Long roomId) {
        Room room = roomRepository.findByIdForUpdate(roomId).orElse(null);
        if (room == null) {
            return Optional.empty();
        }

        if (queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(roomId, QueueItemStatus.WAITING)
                .isPresent()) {
            return Optional.empty();
        }

        Set<String> ineligible = collectIneligibleSongIds(roomId);

        List<Song> eligible = songRepository.findAll().stream()
                .filter(song -> !ineligible.contains(song.getYoutubeVideoId()))
                .toList();

        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        Song chosen = eligible.get(random.nextInt(eligible.size()));
        int position = queueItemRepository.findMaxPositionByRoomId(roomId) + 1;

        QueueItem saved = queueItemRepository.save(new QueueItem(room, chosen, Instant.now(), position));

        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.QUEUE_CHANGED, roomId,
                        new QueueChangedEventPayload(saved.getId(), QueueChangeAction.ADDED)));

        return Optional.of(saved);
    }

    private Set<String> collectIneligibleSongIds(Long roomId) {
        Set<String> ids = new HashSet<>();

        playbackRepository.findByQueueItemRoomIdAndStatus(roomId, PlaybackStatus.PLAYING)
                .ifPresent(playback -> ids.add(playback.getQueueItem().getSong().getYoutubeVideoId()));

        queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(roomId).stream()
                .filter(item -> item.getStatus() == QueueItemStatus.WAITING)
                .map(item -> item.getSong().getYoutubeVideoId())
                .forEach(ids::add);

        ids.addAll(playbackHistoryService.recentSongIds(roomId, RECENT_WINDOW));

        return ids;
    }
}
