package br.com.sintonia.user;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal SintoniaOAuth2User principal) {
        return userService.getMe(requireUserId(principal));
    }

    @PatchMapping("/me")
    public MeResponse updateDisplayName(@AuthenticationPrincipal SintoniaOAuth2User principal,
                                        @RequestBody UpdateDisplayNameRequest request) {
        return userService.updateDisplayName(requireUserId(principal), request.displayName());
    }

    private Long requireUserId(SintoniaOAuth2User principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        return principal.getUserId();
    }
}
