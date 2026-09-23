package br.com.sintonia.room;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class ActiveRoomsController {

    private final RoomDiscoveryService roomDiscoveryService;

    public ActiveRoomsController(RoomDiscoveryService roomDiscoveryService) {
        this.roomDiscoveryService = roomDiscoveryService;
    }

    @GetMapping
    public List<ActiveRoomResponse> listActive() {
        return roomDiscoveryService.listActiveRooms();
    }
}
