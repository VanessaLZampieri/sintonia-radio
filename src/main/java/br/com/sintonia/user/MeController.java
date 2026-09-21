package br.com.sintonia.user;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal SintoniaOAuth2User principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getAvatarUrl());
    }
}
