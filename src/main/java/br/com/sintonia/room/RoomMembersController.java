package br.com.sintonia.room;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomMembersController {

    private final RoomMemberService roomMemberService;

    public RoomMembersController(RoomMemberService roomMemberService) {
        this.roomMemberService = roomMemberService;
    }

    @GetMapping("/{roomId}/members")
    public List<RoomParticipantResponse> present(@PathVariable Long roomId) {
        return roomMemberService.findPresent(roomId);
    }
}
