package br.com.sintonia.queue;

import br.com.sintonia.room.Room;
import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomMemberRepository;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.RoomRepository;
import br.com.sintonia.room.RoomStatus;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.song.Song;
import br.com.sintonia.song.SongNotFoundException;
import br.com.sintonia.song.SongRepository;
import br.com.sintonia.queue.QueueItemNotFoundException;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class QueueService {

    private static final int MAX_SONGS_PER_USER = 8;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SongRepository songRepository;
    private final QueueItemRepository queueItemRepository;
    private final UserRepository userRepository;

    public QueueService(RoomRepository roomRepository,
                        RoomMemberRepository roomMemberRepository,
                        SongRepository songRepository,
                        QueueItemRepository queueItemRepository,
                        UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.songRepository = songRepository;
        this.queueItemRepository = queueItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<QueueItem> findQueue(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return queueItemRepository.findAllByRoomIdOrderByPositionAscIdAsc(roomId);
    }

    @Transactional(readOnly = true)
    public QueueItem findById(Long roomId, Long queueItemId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return queueItemRepository.findByIdAndRoomId(queueItemId, roomId)
                .orElseThrow(() -> new QueueItemNotFoundException("Item da fila não encontrado."));
    }

    @Transactional(readOnly = true)
    public Optional<QueueItem> findPlaying(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return queueItemRepository.findByRoomIdAndStatus(roomId, QueueItemStatus.PLAYING);
    }

    @Transactional(readOnly = true)
    public Optional<QueueItem> findNextWaiting(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return queueItemRepository.findFirstByRoomIdAndStatusOrderByPositionAscIdAsc(roomId, QueueItemStatus.WAITING);
    }

    @Transactional
    public void remove(Long roomId, Long queueItemId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        if (!roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)) {
            throw new UserNotInRoomException("Usuário não está na sala.");
        }

        queueItemRepository.findByIdAndRoomId(queueItemId, roomId)
                .orElseThrow(() -> new QueueItemNotFoundException("Item da fila não encontrado."));

        queueItemRepository.deleteById(queueItemId);
    }

    @Transactional
    public QueueItem add(Long roomId, Long songId, Long userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        if (!roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)) {
            throw new UserNotInRoomException("Usuário não está na sala.");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new SongNotFoundException("Música não encontrada."));

        if (queueItemRepository.existsByRoomIdAndSongId(roomId, songId)) {
            throw new SongAlreadyInQueueException("Esta música já está na fila.");
        }

        if (queueItemRepository.countByRoomIdAndUserId(roomId, userId) >= MAX_SONGS_PER_USER) {
            throw new QueueLimitExceededException(
                    "O usuário já possui " + MAX_SONGS_PER_USER + " músicas na fila.");
        }

        User user = userRepository.getReferenceById(userId);
        int position = queueItemRepository.findMaxPositionByRoomId(roomId) + 1;

        return queueItemRepository.save(new QueueItem(room, song, user, Instant.now(), position));
    }
}
