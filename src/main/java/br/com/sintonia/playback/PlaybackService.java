package br.com.sintonia.playback;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemNotFoundException;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PlaybackService {

    private final RoomRepository roomRepository;
    private final QueueItemRepository queueItemRepository;
    private final PlaybackRepository playbackRepository;

    public PlaybackService(RoomRepository roomRepository,
                           QueueItemRepository queueItemRepository,
                           PlaybackRepository playbackRepository) {
        this.roomRepository = roomRepository;
        this.queueItemRepository = queueItemRepository;
        this.playbackRepository = playbackRepository;
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

        return playback;
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

        return playback;
    }
}