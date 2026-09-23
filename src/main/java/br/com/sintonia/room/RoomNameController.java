package br.com.sintonia.room;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomNameController {

    private final RoomService roomService;

    public RoomNameController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PatchMapping("/{roomId}")
    public RoomResponse rename(@PathVariable Long roomId,
                               @RequestBody RenameRoomRequest request,
                               @AuthenticationPrincipal SintoniaOAuth2User principal) {
        return roomService.rename(roomId, request.name(), principal.getUserId());
    }
}
