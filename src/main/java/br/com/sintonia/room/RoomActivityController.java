package br.com.sintonia.room;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomActivityController {

    private final RoomActivityService roomActivityService;

    public RoomActivityController(RoomActivityService roomActivityService) {
        this.roomActivityService = roomActivityService;
    }

    @GetMapping("/{roomId}/activities")
    public List<RoomActivityResponse> list(@PathVariable Long roomId) {
        return roomActivityService.findRecent(roomId);
    }
}
