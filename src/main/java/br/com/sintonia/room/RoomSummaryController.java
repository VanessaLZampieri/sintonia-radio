package br.com.sintonia.room;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomSummaryController {

    private final RoomSummaryService roomSummaryService;

    public RoomSummaryController(RoomSummaryService roomSummaryService) {
        this.roomSummaryService = roomSummaryService;
    }

    @GetMapping("/{roomId}/summary")
    public RoomSummaryResponse get(@PathVariable Long roomId,
                                   @AuthenticationPrincipal SintoniaOAuth2User principal) {
        return roomSummaryService.get(roomId, principal.getUserId());
    }
}
