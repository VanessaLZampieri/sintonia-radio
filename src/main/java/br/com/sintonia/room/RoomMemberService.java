package br.com.sintonia.room;

import br.com.sintonia.user.User;
import br.com.sintonia.user.UserNotFoundException;
import br.com.sintonia.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class RoomMemberService {

    public static final int MAX_PARTICIPANTS = 15;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final RoomActivityService roomActivityService;

    public RoomMemberService(RoomRepository roomRepository,
                             RoomMemberRepository roomMemberRepository,
                             UserRepository userRepository,
                             RoomActivityService roomActivityService) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.roomActivityService = roomActivityService;
    }

    @Transactional
    public RoomMemberResponse enterRoom(String rawCode, Long userId) {
        String code = rawCode.toUpperCase(Locale.ROOT);

        Room room = roomRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        Optional<RoomMember> existing = roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user);
        if (existing.isPresent()) {
            return RoomMemberResponse.from(existing.get());
        }

        if (roomMemberRepository.countByRoomAndLeftAtIsNull(room) >= MAX_PARTICIPANTS) {
            throw new RoomFullException(
                    "Sala cheia. Esta sala já possui " + MAX_PARTICIPANTS + " participantes.");
        }

        RoomMember member = roomMemberRepository.save(new RoomMember(room, user));
        room.markOccupied();
        roomActivityService.record(room, RoomActivityType.MEMBER_JOINED, user, null, null);
        return RoomMemberResponse.from(member);
    }

    @Transactional
    public Optional<RoomMemberResponse> leaveRoom(String rawCode, Long userId) {
        String code = rawCode.toUpperCase(Locale.ROOT);

        Room room = roomRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));

        Optional<RoomMember> member = roomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user);
        if (member.isEmpty()) {
            return Optional.empty();
        }

        member.get().leave();
        roomActivityService.record(room, RoomActivityType.MEMBER_LEFT, user, null, null);
        if (room.getStatus() == RoomStatus.ACTIVE
                && roomMemberRepository.countByRoomAndLeftAtIsNull(room) == 0) {
            room.markEmpty();
        }

        return Optional.of(RoomMemberResponse.from(member.get()));
    }

    @Transactional(readOnly = true)
    public List<RoomParticipantResponse> findPresent(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        return roomMemberRepository.findAllByRoomIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(roomId).stream()
                .map(RoomParticipantResponse::from)
                .toList();
    }
}
