package br.com.sintonia.room;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rooms")
public class RoomPlaybackModeController {

    private final RoomPlaybackModeService roomPlaybackModeService;

    public RoomPlaybackModeController(RoomPlaybackModeService roomPlaybackModeService) {
        this.roomPlaybackModeService = roomPlaybackModeService;
    }

    @GetMapping("/{roomId}/playback-mode")
    public PlaybackModeResponse current(@PathVariable Long roomId) {
        return roomPlaybackModeService.current(roomId);
    }

    @PutMapping("/{roomId}/playback-mode")
    public PlaybackModeResponse change(@PathVariable Long roomId, @RequestBody ChangePlaybackModeRequest request) {
        if (request.mode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode é obrigatório.");
        }
        return roomPlaybackModeService.change(roomId, request.mode());
    }
}
