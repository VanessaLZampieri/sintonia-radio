package br.com.sintonia.room;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/rooms")
public class RoomMemberController {

    private final RoomMemberService roomMemberService;

    public RoomMemberController(RoomMemberService roomMemberService) {
        this.roomMemberService = roomMemberService;
    }

    @PostMapping("/{code}/members")
    public RoomMemberResponse enterRoom(@PathVariable String code, @RequestBody EnterRoomRequest request) {
        return roomMemberService.enterRoom(code, request.userId());
    }

    @DeleteMapping("/{code}/members/{userId}")
    public ResponseEntity<?> leaveRoom(@PathVariable String code, @PathVariable Long userId) {
        Optional<RoomMemberResponse> result = roomMemberService.leaveRoom(code, userId);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.noContent().build();
    }
}
