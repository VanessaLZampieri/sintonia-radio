package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.song.Song;
import br.com.sintonia.song.SongRepository;
import br.com.sintonia.song.SongService;
import br.com.sintonia.song.SongDurationPolicy;
import br.com.sintonia.song.YouTubeSongService;
import br.com.sintonia.youtube.YouTubeVideoDetails;
import br.com.sintonia.websocket.QueueChangeAction;
import br.com.sintonia.websocket.QueueChangedEventPayload;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import br.com.sintonia.websocket.RoomEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
public class AutoDjService {

    private static final int RECENT_WINDOW = 10;
    private static final int CONTEXTUAL_MAX_RESULTS = 10;

    private final RoomRepository roomRepository;
    private final SongRepository songRepository;
    private final QueueItemRepository queueItemRepository;
    private final PlaybackRepository playbackRepository;
    private final PlaybackHistoryService playbackHistoryService;
    private final RoomEventPublisher roomEventPublisher;
    private final SongService songService;
    private final YouTubeSongService youTubeSongService;
    private final Random random;

    @Autowired
    public AutoDjService(RoomRepository roomRepository,
                         SongRepository songRepository,
                         QueueItemRepository queueItemRepository,
                         PlaybackRepository playbackRepository,
                         PlaybackHistoryService playbackHistoryService,
                         RoomEventPublisher roomEventPublisher,
                         SongService songService,
                         YouTubeSongService youTubeSongService) {
        this(roomRepository, songRepository, queueItemRepository, playbackRepository,
                playbackHistoryService, roomEventPublisher, songService, youTubeSongService, new Random());
    }

    AutoDjService(RoomRepository roomRepository,
                  SongRepository songRepository,
                  QueueItemRepository queueItemRepository,
                  PlaybackRepository playbackRepository,
                  PlaybackHistoryService playbackHistoryService,
                  RoomEventPublisher roomEventPublisher,
                  SongService songService,
                  YouTubeSongService youTubeSongService,
                  Random random) {
        this.roomRepository = roomRepository;
        this.songRepository = songRepository;
        this.queueItemRepository = queueItemRepository;
        this.playbackRepository = playbackRepository;
        this.playbackHistoryService = playbackHistoryService;
        this.roomEventPublisher = roomEventPublisher;
        this.songService = songService;
        this.youTubeSongService = youTubeSongService;
        this.random = random;
    }

    @Transactional
    public Optional<QueueItem> createNext(Long roomId) {
        if (queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(roomId, QueueItemStatus.WAITING)
                .isPresent()) {
            return Optional.empty();
        }

        Optional<Song> seed = lastFinishedSong(roomId);
        List<YouTubeVideoDetails> contextual = seed.map(this::searchContextual).orElse(List.of());

        Room room = roomRepository.findByIdForUpdate(roomId).orElse(null);
        if (room == null) {
            return Optional.empty();
        }

        // Reconfirma sob o lock pessimista que a fila continua sem WAITING.
        if (queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(roomId, QueueItemStatus.WAITING)
                .isPresent()) {
            return Optional.empty();
        }

        Set<String> ineligible = collectIneligibleSongIds(roomId);
        seed.ifPresent(s -> ineligible.add(s.getYoutubeVideoId()));

        for (YouTubeVideoDetails candidate : contextual) {
            if (ineligible.contains(candidate.videoId())
                    || !SongDurationPolicy.isAllowed(candidate.duration())) {
                continue;
            }
            return Optional.of(persistRecommended(room, candidate));
        }

        return fallbackFromDatabase(room, ineligible);
    }

    private Optional<Song> lastFinishedSong(Long roomId) {
        return playbackRepository.findFirstByQueueItemRoomIdAndStatusOrderByStartedAtDesc(
                        roomId, PlaybackStatus.FINISHED)
                .map(playback -> playback.getQueueItem().getSong());
    }

    private List<YouTubeVideoDetails> searchContextual(Song seed) {
        String query = buildQuery(seed);
        try {
            return youTubeSongService.searchDetails(query, CONTEXTUAL_MAX_RESULTS);
        } catch (RestClientException exception) {
            return List.of();
        }
    }

    private String buildQuery(Song seed) {
        String channel = seed.getChannelTitle();
        String title = seed.getTitle();
        if (channel != null && !channel.isBlank()) {
            return (channel + " " + title).trim();
        }
        return title;
    }

    private QueueItem persistRecommended(Room room, YouTubeVideoDetails candidate) {
        Song song = songService.findOrCreate(
                candidate.videoId(),
                candidate.title(),
                candidate.thumbnailUrl(),
                candidate.duration(),
                candidate.channelTitle());
        return persistAutoDjItem(room, song);
    }

    private Optional<QueueItem> fallbackFromDatabase(Room room, Set<String> ineligible) {
        List<Song> eligible = songRepository.findAll().stream()
                .filter(song -> !ineligible.contains(song.getYoutubeVideoId()))
                .filter(song -> SongDurationPolicy.isAllowed(song.getDuration()))
                .toList();

        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        Song chosen = eligible.get(random.nextInt(eligible.size()));
        return Optional.of(persistAutoDjItem(room, chosen));
    }

    private QueueItem persistAutoDjItem(Room room, Song song) {
        int position = queueItemRepository.findMaxPositionByRoomId(room.getId()) + 1;
        QueueItem saved = queueItemRepository.save(new QueueItem(room, song, Instant.now(), position));

        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.QUEUE_CHANGED, room.getId(),
                        new QueueChangedEventPayload(saved.getId(), QueueChangeAction.ADDED)));

        return saved;
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
