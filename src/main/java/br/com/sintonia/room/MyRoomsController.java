package br.com.sintonia.room;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MyRoomsController {

    private final RoomDiscoveryService roomDiscoveryService;

    public MyRoomsController(RoomDiscoveryService roomDiscoveryService) {
        this.roomDiscoveryService = roomDiscoveryService;
    }

    @GetMapping("/me/rooms")
    public List<UserRoomResponse> listMine(@AuthenticationPrincipal SintoniaOAuth2User principal) {
        return roomDiscoveryService.listUserRooms(principal.getUserId());
    }
}
