package br.com.sintonia.room;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RoomLifecycleScheduler {

    private final RoomLifecycleService roomLifecycleService;

    public RoomLifecycleScheduler(RoomLifecycleService roomLifecycleService) {
        this.roomLifecycleService = roomLifecycleService;
    }

    @Scheduled(fixedDelay = 60000)
    public void closeExpiredRooms() {
        roomLifecycleService.closeExpiredRooms();
    }
}
