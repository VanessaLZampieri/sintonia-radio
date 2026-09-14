package br.com.sintonia.room;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomPlayerController {

    private final RoomPlayerService roomPlayerService;

    public RoomPlayerController(RoomPlayerService roomPlayerService) {
        this.roomPlayerService = roomPlayerService;
    }

    @GetMapping("/{roomId}/player")
    public RoomPlayerResponse current(@PathVariable Long roomId) {
        return roomPlayerService.current(roomId);
    }

    @PostMapping("/{roomId}/player")
    public RoomPlayerResponse claim(@PathVariable Long roomId, @RequestBody ClaimPlayerRequest request) {
        return roomPlayerService.claim(roomId, request.clientSessionId(), request.userId());
    }

    @DeleteMapping("/{roomId}/player")
    public RoomPlayerResponse release(@PathVariable Long roomId, @RequestBody ReleasePlayerRequest request) {
        return roomPlayerService.release(roomId, request.clientSessionId());
    }
}
