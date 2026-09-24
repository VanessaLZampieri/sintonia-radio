package br.com.sintonia.room;

import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.user.User;
import br.com.sintonia.user.UserNotFoundException;
import br.com.sintonia.user.UserRepository;
import br.com.sintonia.websocket.RoomEvent;
import br.com.sintonia.websocket.RoomEventPublisher;
import br.com.sintonia.websocket.RoomEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomMemberService {

    public static final int MAX_PARTICIPANTS = 15;
    static final Duration PRESENCE_LEASE = Duration.ofSeconds(90);

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomPresenceRepository roomPresenceRepository;
    private final UserRepository userRepository;
    private final QueueItemRepository queueItemRepository;
    private final RoomActivityService roomActivityService;
    private final RoomEventPublisher roomEventPublisher;
    private final RoomPlayerService roomPlayerService;

    public RoomMemberService(RoomRepository roomRepository,
                             RoomMemberRepository roomMemberRepository,
                             RoomPresenceRepository roomPresenceRepository,
                             UserRepository userRepository,
                             QueueItemRepository queueItemRepository,
                             RoomActivityService roomActivityService,
                             RoomEventPublisher roomEventPublisher,
                             RoomPlayerService roomPlayerService) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomPresenceRepository = roomPresenceRepository;
        this.userRepository = userRepository;
        this.queueItemRepository = queueItemRepository;
        this.roomActivityService = roomActivityService;
        this.roomEventPublisher = roomEventPublisher;
        this.roomPlayerService = roomPlayerService;
    }

    @Transactional
    public RoomMemberResponse enterRoom(String rawCode, Long userId, String clientSessionId) {
        validateClientSessionId(clientSessionId);
        String code = rawCode.toUpperCase(Locale.ROOT);

        Room room = roomRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        Instant now = Instant.now();
        Optional<RoomMember> existing = roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user);
        boolean alreadyPresent = roomPresenceRepository
                .existsByRoomIdAndUserIdAndExpiresAtAfter(room.getId(), userId, now);
        if (!alreadyPresent && roomMemberRepository.countByRoomAndLeftAtIsNull(room) >= MAX_PARTICIPANTS) {
            throw new RoomFullException(
                    "Sala cheia. Esta sala já possui " + MAX_PARTICIPANTS + " participantes.");
        }

        RoomMember member = existing.orElseGet(() -> {
            RoomMember created = roomMemberRepository.save(new RoomMember(room, user));
            roomActivityService.record(room, RoomActivityType.MEMBER_JOINED, user, null, null);
            return created;
        });
        Instant expiresAt = now.plus(PRESENCE_LEASE);
        roomPresenceRepository.findByRoomAndUserAndClientSessionId(room, user, clientSessionId)
                .ifPresentOrElse(
                        presence -> presence.renew(expiresAt),
                         () -> roomPresenceRepository.save(
                                 new RoomPresence(room, user, member, clientSessionId, expiresAt)));
        if (!alreadyPresent) {
            publishMembersChanged(room);
        }
        room.markOccupied();
        return RoomMemberResponse.from(member);
    }

    @Transactional
    public Optional<RoomMemberResponse> leaveRoom(String rawCode, Long userId, String clientSessionId) {
        validateClientSessionId(clientSessionId);
        String code = rawCode.toUpperCase(Locale.ROOT);

        Room room = roomRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        Optional<RoomPresence> presence = roomPresenceRepository
                .findByRoomAndUserAndClientSessionId(room, user, clientSessionId);
        if (presence.isEmpty()) {
            return Optional.empty();
        }

        RoomMember member = presence.get().getMember();
        roomPresenceRepository.delete(presence.get());
        roomPresenceRepository.flush();
        roomPlayerService.releaseIfClaimed(room.getId(), clientSessionId);
        finishMembershipIfAbsent(room, user, member, Instant.now());

        return Optional.of(RoomMemberResponse.from(member));
    }

    @Transactional
    public void expireStalePresences() {
        Instant now = Instant.now();
        for (Object[] candidate : roomPresenceRepository.findExpiredCandidates(now)) {
            expirePresence((Long) candidate[0], (Long) candidate[1], now);
        }
        for (Long memberId : roomMemberRepository.findOpenMemberIdsWithoutPresence()) {
            RoomMember member = roomMemberRepository.findById(memberId).orElse(null);
            if (member == null || member.getLeftAt() != null) {
                continue;
            }
            Room room = roomRepository.findByIdForUpdate(member.getRoom().getId()).orElse(null);
            if (room != null) {
                finishMembershipIfAbsent(room, member.getUser(), member, now);
            }
        }
    }

    private void expirePresence(Long presenceId, Long roomId, Instant now) {
        Room room = roomRepository.findByIdForUpdate(roomId).orElse(null);
        if (room == null) {
            return;
        }
        RoomPresence presence = roomPresenceRepository.findById(presenceId).orElse(null);
        if (presence == null || presence.getExpiresAt().isAfter(now)) {
            return;
        }
        User user = presence.getUser();
        RoomMember member = presence.getMember();
        roomPresenceRepository.delete(presence);
        roomPresenceRepository.flush();
        roomPlayerService.releaseIfClaimed(room.getId(), presence.getClientSessionId());
        finishMembershipIfAbsent(room, user, member, now);
    }

    private void finishMembershipIfAbsent(Room room, User user, RoomMember member, Instant now) {
        if (roomPresenceRepository.existsByRoomIdAndUserIdAndExpiresAtAfter(room.getId(), user.getId(), now)
                || member.getLeftAt() != null) {
            return;
        }
        roomPlayerService.releaseIfClaimedByUser(room.getId(), user.getId());
        member.leave();
        roomActivityService.record(room, RoomActivityType.MEMBER_LEFT, user, null, null);
        publishMembersChanged(room);
        if (room.getStatus() == RoomStatus.ACTIVE
                && roomPresenceRepository.countPresentUsers(room, now) == 0) {
            room.markEmpty();
        }
    }

    private void validateClientSessionId(String clientSessionId) {
        try {
            UUID.fromString(clientSessionId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidClientSessionIdException("clientSessionId inválido.");
        }
    }

    private void publishMembersChanged(Room room) {
        roomEventPublisher.publish(room.getCode(),
                new RoomEvent(RoomEventType.MEMBERS_CHANGED, room.getId(), null));
    }

    @Transactional(readOnly = true)
    public List<RoomParticipantResponse> findPresent(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        Map<Long, Long> waitingCounts = queueItemRepository
                .countWaitingByUser(roomId, QueueItemStatus.WAITING).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return roomMemberRepository.findAllByRoomIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(roomId).stream()
                .map(member -> RoomParticipantResponse.from(
                        member, waitingCounts.getOrDefault(member.getUser().getId(), 0L)))
                .toList();
    }
}
