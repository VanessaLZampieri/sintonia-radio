package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemSource;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.song.SongRepository;
import br.com.sintonia.song.SongService;
import br.com.sintonia.song.YouTubeSongService;
import br.com.sintonia.youtube.YouTubeVideoDetails;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoDjServiceTest {

    private static final Long ROOM_ID = 10L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private QueueItemRepository queueItemRepository;

    @Mock
    private PlaybackRepository playbackRepository;

    @Mock
    private PlaybackHistoryService playbackHistoryService;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    @Mock
    private SongService songService;

    @Mock
    private YouTubeSongService youTubeSongService;

    private Random random;
    private AutoDjService autoDjService;

    private Room room;
    private Song song1;
    private Song song2;

    @BeforeEach
    void setUp() {
        room = new Room("Sala Teste", "ABCDEFGH", RoomStatus.ACTIVE);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        song1 = new Song("song-1", "Song 1", null, Duration.ofSeconds(100), "Channel 1");
        song2 = new Song("song-2", "Song 2", null, Duration.ofSeconds(100), "Channel 2");
        random = mock(Random.class);
        autoDjService = new AutoDjService(roomRepository, songRepository, queueItemRepository,
                playbackRepository, playbackHistoryService, roomEventPublisher,
                songService, youTubeSongService, random);
    }

    @Test
    void usesContextualSearchWhenFinishedSeedExists() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails("Channel 1 Song 1", 10))
                .thenReturn(List.of(details("song-2")));

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-2");
        assertThat(result.getSource()).isEqualTo(QueueItemSource.AUTO_DJ);
        verify(youTubeSongService).searchDetails("Channel 1 Song 1", 10);
    }

    @Test
    void buildsQueryWithChannelAndTitle() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt())).thenReturn(List.of());

        autoDjService.createNext(ROOM_ID);

        verify(youTubeSongService).searchDetails("Channel 1 Song 1", 10);
    }

    @Test
    void buildsQueryWithTitleOnlyWhenNoChannel() {
        stubBase();
        Song noChannel = new Song("song-1", "Song 1", null, Duration.ofSeconds(100));
        stubSeed(noChannel);
        when(youTubeSongService.searchDetails(anyString(), anyInt())).thenReturn(List.of());

        autoDjService.createNext(ROOM_ID);

        verify(youTubeSongService).searchDetails("Song 1", 10);
    }

    @Test
    void picksFirstEligibleCandidateByRelevance() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt()))
                .thenReturn(List.of(details("song-a"), details("song-b"), details("song-c")));
        when(playbackHistoryService.recentSongIds(ROOM_ID, 10)).thenReturn(List.of("song-a"));

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-b");
    }

    @Test
    void ignoresWaitingResult() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt()))
                .thenReturn(List.of(details("song-a"), details("song-b")));
        when(queueItemRepository.findAllByRoomIdAndStatusOrderByPositionAscIdAsc(
                ROOM_ID, QueueItemStatus.WAITING))
                .thenReturn(List.of(new QueueItem(room, songFor("song-a"), Instant.now(), 1)));

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-b");
    }

    @Test
    void ignoresPlayingResult() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt()))
                .thenReturn(List.of(details("song-a"), details("song-b")));
        QueueItem playing = new QueueItem(room, songFor("song-a"), Instant.now(), 1);
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.of(new Playback(playing, Instant.now())));

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-b");
    }

    @Test
    void ignoresSeedVideoId() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt()))
                .thenReturn(List.of(details("song-1"), details("song-2")));

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-2");
    }

    @Test
    void youtubeFailureFallsBackToDatabase() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt()))
                .thenThrow(new RestClientException("down"));
        when(songRepository.findAll()).thenReturn(List.of(song2));
        when(random.nextInt(anyInt())).thenReturn(0);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-2");
    }

    @Test
    void emptySearchFallsBackToDatabase() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt())).thenReturn(List.of());
        when(songRepository.findAll()).thenReturn(List.of(song2));
        when(random.nextInt(anyInt())).thenReturn(0);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-2");
    }

    @Test
    void contextualSearchSkipsCandidateLongerThanTwentyMinutes() {
        stubBase();
        stubSeed(song1);
        YouTubeVideoDetails longVideo = new YouTubeVideoDetails(
                "long", "Long", "Channel", null, Duration.ofMinutes(21), true, false);
        when(youTubeSongService.searchDetails(anyString(), anyInt()))
                .thenReturn(List.of(longVideo, details("song-2")));

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-2");
    }

    @Test
    void databaseFallbackRejectsSongsLongerThanTwentyMinutes() {
        stubBase();
        Song longSong = new Song("long", "Long", null, Duration.ofMinutes(21));
        when(songRepository.findAll()).thenReturn(List.of(longSong));

        assertThat(autoDjService.createNext(ROOM_ID)).isEmpty();
        verify(queueItemRepository, never()).save(any());
    }

    @Test
    void allCandidatesIneligibleFallsBackToDatabase() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt())).thenReturn(List.of(details("song-a")));
        when(playbackHistoryService.recentSongIds(ROOM_ID, 10)).thenReturn(List.of("song-a"));
        when(songRepository.findAll()).thenReturn(List.of(song2));
        when(random.nextInt(anyInt())).thenReturn(0);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-2");
    }

    @Test
    void noFinishedSeedFallsBackWithoutContextualSearch() {
        stubBase();
        when(songRepository.findAll()).thenReturn(List.of(song1));
        when(random.nextInt(anyInt())).thenReturn(0);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-1");
        verify(youTubeSongService, never()).searchDetails(anyString(), anyInt());
    }

    @Test
    void fallbackChoosesRandomly() {
        stubBase();
        when(songRepository.findAll()).thenReturn(List.of(song1, song2));
        when(random.nextInt(2)).thenReturn(1);

        QueueItem result = autoDjService.createNext(ROOM_ID).orElseThrow();

        assertThat(result.getSong().getYoutubeVideoId()).isEqualTo("song-2");
    }

    @Test
    void humanWaitingPreventsAutoDj() {
        stubBase();
        when(queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING))
                .thenReturn(Optional.of(new QueueItem(room, song1, Instant.now(), 1)));

        assertThat(autoDjService.createNext(ROOM_ID)).isEmpty();
        verify(youTubeSongService, never()).searchDetails(anyString(), anyInt());
        verify(songRepository, never()).findAll();
    }

    @Test
    void persistsChannelTitleThroughFindOrCreate() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt()))
                .thenReturn(List.of(new YouTubeVideoDetails(
                        "song-2", "Song 2", "Channel 2", null, Duration.ofSeconds(100), true, false)));

        autoDjService.createNext(ROOM_ID).orElseThrow();

        verify(songService).findOrCreate("song-2", "Song 2", null, Duration.ofSeconds(100), "Channel 2");
    }

    @Test
    void failureDoesNotBreakWhenNoFallback() {
        stubBase();
        stubSeed(song1);
        when(youTubeSongService.searchDetails(anyString(), anyInt()))
                .thenThrow(new RestClientException("down"));
        when(songRepository.findAll()).thenReturn(List.of());

        assertThat(autoDjService.createNext(ROOM_ID)).isEmpty();
    }

    @Test
    void publishesQueueChangedEvent() {
        stubBase();
        when(songRepository.findAll()).thenReturn(List.of(song1));
        when(random.nextInt(anyInt())).thenReturn(0);

        autoDjService.createNext(ROOM_ID);

        verify(roomEventPublisher).publish(eq("ABCDEFGH"), any(RoomEvent.class));
    }

    private void stubBase() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(ROOM_ID, QueueItemStatus.WAITING))
                .thenReturn(Optional.empty());
        when(playbackRepository.findFirstByQueueItemRoomIdAndStatusOrderByStartedAtDesc(
                ROOM_ID, PlaybackStatus.FINISHED)).thenReturn(Optional.empty());
        when(playbackRepository.findByQueueItemRoomIdAndStatus(ROOM_ID, PlaybackStatus.PLAYING))
                .thenReturn(Optional.empty());
        when(queueItemRepository.findAllByRoomIdAndStatusOrderByPositionAscIdAsc(
                ROOM_ID, QueueItemStatus.WAITING)).thenReturn(List.of());
        when(playbackHistoryService.recentSongIds(ROOM_ID, 10)).thenReturn(List.of());
        when(songRepository.findAll()).thenReturn(List.of());
        when(queueItemRepository.findMaxPositionByRoomId(ROOM_ID)).thenReturn(0);
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(inv -> {
            QueueItem saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            return saved;
        });
        when(songService.findOrCreate(anyString(), anyString(), any(), any(), any()))
                .thenAnswer(inv -> new Song(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                        inv.getArgument(3), inv.getArgument(4)));
    }

    private void stubSeed(Song song) {
        QueueItem item = new QueueItem(room, song, Instant.now(), 1);
        when(playbackRepository.findFirstByQueueItemRoomIdAndStatusOrderByStartedAtDesc(
                ROOM_ID, PlaybackStatus.FINISHED)).thenReturn(Optional.of(new Playback(item, Instant.now())));
    }

    private YouTubeVideoDetails details(String videoId) {
        return new YouTubeVideoDetails(videoId, "Title " + videoId, "Channel", null,
                Duration.ofSeconds(100), true, false);
    }

    private Song songFor(String videoId) {
        return new Song(videoId, "Title " + videoId, null, Duration.ofSeconds(100));
    }
}
