package br.com.sintonia.room;

import br.com.sintonia.queue.QueueItem;
import br.com.sintonia.queue.QueueItemRepository;
import br.com.sintonia.queue.QueueItemStatus;
import br.com.sintonia.song.Song;
import br.com.sintonia.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomDiscoveryService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final QueueItemRepository queueItemRepository;

    public RoomDiscoveryService(RoomRepository roomRepository,
                                RoomMemberRepository roomMemberRepository,
                                QueueItemRepository queueItemRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.queueItemRepository = queueItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ActiveRoomResponse> listActiveRooms() {
        return roomRepository.findActiveRoomsWithPresentMembers(RoomStatus.ACTIVE).stream()
                .map(this::toActiveRoomResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserRoomResponse> listUserRooms(Long userId) {
        List<RoomMember> members = roomMemberRepository.findByUserIdOrderByJoinedAtDesc(userId);
        Map<Long, Room> distinctRooms = new LinkedHashMap<>();
        for (RoomMember member : members) {
            distinctRooms.putIfAbsent(member.getRoom().getId(), member.getRoom());
        }
        return distinctRooms.values().stream()
                .map(this::toUserRoomResponse)
                .toList();
    }

    private ActiveRoomResponse toActiveRoomResponse(Room room) {
        long participantCount = roomMemberRepository.countByRoomAndLeftAtIsNull(room);
        long waitingCount = queueItemRepository.countByRoomIdAndStatus(room.getId(), QueueItemStatus.WAITING);
        ActiveRoomResponse.NowPlaying nowPlaying = queueItemRepository
                .findByRoomIdAndStatus(room.getId(), QueueItemStatus.PLAYING)
                .map(this::toNowPlaying)
                .orElse(null);
        return new ActiveRoomResponse(
                room.getId(),
                room.getName(),
                room.getCode(),
                participantCount,
                waitingCount,
                nowPlaying);
    }

    private ActiveRoomResponse.NowPlaying toNowPlaying(QueueItem item) {
        Song song = item.getSong();
        User user = item.getUser();
        ActiveRoomResponse.AddedBy addedBy = user == null
                ? null
                : new ActiveRoomResponse.AddedBy(user.getId(), user.getDisplayName(), user.getAvatarUrl());
        return new ActiveRoomResponse.NowPlaying(
                song.getTitle(),
                song.getYoutubeVideoId(),
                song.getThumbnailUrl(),
                addedBy);
    }

    private UserRoomResponse toUserRoomResponse(Room room) {
        long participantCount = roomMemberRepository.countByRoomAndLeftAtIsNull(room);
        boolean canEnter = room.getStatus() == RoomStatus.ACTIVE
                && participantCount < RoomMemberService.MAX_PARTICIPANTS;
        return new UserRoomResponse(
                room.getId(),
                room.getName(),
                room.getCode(),
                room.getStatus(),
                canEnter,
                participantCount);
    }
}
