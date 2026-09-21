package br.com.sintonia.room;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomStateController {

    private final RoomStateService roomStateService;

    public RoomStateController(RoomStateService roomStateService) {
        this.roomStateService = roomStateService;
    }

    @GetMapping("/{roomId}/state")
    public RoomStateResponse get(@PathVariable Long roomId,
                                 @AuthenticationPrincipal SintoniaOAuth2User principal) {
        return roomStateService.get(roomId, principal.getUserId());
    }
}
