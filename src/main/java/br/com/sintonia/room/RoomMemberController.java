package br.com.sintonia.room;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public RoomMemberResponse enterRoom(@PathVariable String code,
                                        @AuthenticationPrincipal SintoniaOAuth2User principal) {
        return roomMemberService.enterRoom(code, principal.getUserId());
    }

    @DeleteMapping("/{code}/members")
    public ResponseEntity<?> leaveRoom(@PathVariable String code,
                                       @AuthenticationPrincipal SintoniaOAuth2User principal) {
        Optional<RoomMemberResponse> result = roomMemberService.leaveRoom(code, principal.getUserId());
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.noContent().build();
    }
}
