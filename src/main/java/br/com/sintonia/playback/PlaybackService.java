package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemNotFoundException;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.queue.QueueService;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.websocket.PlaybackEventPayload;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import br.com.sintonia.websocket.RoomEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class PlaybackService {

    private final RoomRepository roomRepository;
    private final QueueItemRepository queueItemRepository;
    private final PlaybackRepository playbackRepository;
    private final QueueService queueService;
    private final RoomEventPublisher roomEventPublisher;
    private final AutoDjService autoDjService;

    public PlaybackService(RoomRepository roomRepository,
                           QueueItemRepository queueItemRepository,
                           PlaybackRepository playbackRepository,
                           QueueService queueService,
                           RoomEventPublisher roomEventPublisher,
                           AutoDjService autoDjService) {
        this.roomRepository = roomRepository;
        this.queueItemRepository = queueItemRepository;
        this.playbackRepository = playbackRepository;
        this.queueService = queueService;
        this.roomEventPublisher = roomEventPublisher;
        this.autoDjService = autoDjService;
    }

    @Transactional
    public Playback start(Long roomId, Long queueItemId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        QueueItem queueItem = queueItemRepository.findByIdAndRoomId(queueItemId, roomId)
                .orElseThrow(() -> new QueueItemNotFoundException("Item da fila não encontrado."));

        if (queueItem.getStatus() != QueueItemStatus.WAITING) {
            throw new QueueItemNotWaitingException("O item da fila não está aguardando reprodução.");
        }

        if (playbackRepository.findByQueueItemRoomIdAndStatus(roomId, PlaybackStatus.PLAYING).isPresent()) {
            throw new PlaybackAlreadyInProgressException("Já existe uma reprodução em andamento nesta sala.");
        }

        queueItem.setStatus(QueueItemStatus.PLAYING);

        Playback playback = new Playback(queueItem, Instant.now());

        queueItemRepository.save(queueItem);
        playbackRepository.save(playback);

        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.PLAYBACK_STARTED, roomId,
                        new PlaybackEventPayload(playback.getId(), queueItemId)));

        return playback;
    }

    @Transactional
    public Optional<Playback> startNext(Long roomId) {
        Optional<QueueItem> next = queueService.findNextWaiting(roomId);
        if (next.isEmpty()) {
            next = autoDjService.createNext(roomId);
        }
        if (next.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(start(roomId, next.get().getId()));
    }

    @Transactional
    public Playback finish(Long playbackId) {
        Playback playback = playbackRepository.findById(playbackId)
                .orElseThrow(() -> new PlaybackNotFoundException("Playback não encontrado."));

        if (playback.getStatus() != PlaybackStatus.PLAYING) {
            throw new PlaybackNotPlayingException("O playback não está em andamento.");
        }

        playback.setStatus(PlaybackStatus.FINISHED);
        playback.setEndedAt(Instant.now());

        QueueItem queueItem = playback.getQueueItem();
        queueItem.setStatus(QueueItemStatus.FINISHED);

        playbackRepository.save(playback);
        queueItemRepository.save(queueItem);

        Room room = queueItem.getRoom();
        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.PLAYBACK_FINISHED, room.getId(),
                        new PlaybackEventPayload(playbackId, queueItem.getId())));

        startNext(room.getId());

        return playback;
    }

    @Transactional
    public Playback skip(Long playbackId) {
        Playback playback = playbackRepository.findById(playbackId)
                .orElseThrow(() -> new PlaybackNotFoundException("Playback não encontrado."));

        if (playback.getStatus() != PlaybackStatus.PLAYING) {
            throw new PlaybackNotPlayingException("O playback não está em andamento.");
        }

        playback.setStatus(PlaybackStatus.SKIPPED);
        playback.setEndedAt(Instant.now());

        QueueItem queueItem = playback.getQueueItem();
        queueItem.setStatus(QueueItemStatus.SKIPPED);

        playbackRepository.save(playback);
        queueItemRepository.save(queueItem);

        Room room = queueItem.getRoom();
        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.PLAYBACK_SKIPPED, room.getId(),
                        new PlaybackEventPayload(playbackId, queueItem.getId())));

        startNext(room.getId());

        return playback;
    }

    @Transactional
    public Playback error(Long playbackId) {
        Playback playback = playbackRepository.findById(playbackId)
                .orElseThrow(() -> new PlaybackNotFoundException("Playback não encontrado."));

        if (playback.getStatus() != PlaybackStatus.PLAYING) {
            throw new PlaybackNotPlayingException("O playback não está em andamento.");
        }

        playback.setStatus(PlaybackStatus.ERROR);
        playback.setEndedAt(Instant.now());

        QueueItem queueItem = playback.getQueueItem();
        queueItem.setStatus(QueueItemStatus.ERROR);

        playbackRepository.save(playback);
        queueItemRepository.save(queueItem);

        Room room = queueItem.getRoom();
        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.PLAYBACK_ERROR, room.getId(),
                        new PlaybackEventPayload(playbackId, queueItem.getId())));

        startNext(room.getId());

        return playback;
    }
}