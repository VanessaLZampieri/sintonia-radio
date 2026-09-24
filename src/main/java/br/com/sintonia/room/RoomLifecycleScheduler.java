package br.com.sintonia.room;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RoomLifecycleScheduler {

    private final RoomLifecycleService roomLifecycleService;
    private final RoomMemberService roomMemberService;

    public RoomLifecycleScheduler(RoomLifecycleService roomLifecycleService,
                                  RoomMemberService roomMemberService) {
        this.roomLifecycleService = roomLifecycleService;
        this.roomMemberService = roomMemberService;
    }

    @Scheduled(fixedDelay = 30000)
    public void closeExpiredRooms() {
        roomMemberService.expireStalePresences();
        roomLifecycleService.closeExpiredRooms();
    }
}
