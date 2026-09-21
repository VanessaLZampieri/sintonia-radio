package br.com.sintonia.playback;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rooms")
public class SkipVoteController {

    private final SkipVoteService skipVoteService;

    public SkipVoteController(SkipVoteService skipVoteService) {
        this.skipVoteService = skipVoteService;
    }

    @PostMapping("/{roomId}/skip-votes")
    public SkipVoteResponse vote(@PathVariable Long roomId,
                                 @AuthenticationPrincipal SintoniaOAuth2User principal) {
        return skipVoteService.vote(roomId, principal.getUserId());
    }

    @GetMapping("/{roomId}/skip-votes")
    public SkipVoteResponse current(@PathVariable Long roomId) {
        return skipVoteService.current(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não há playback em andamento."));
    }
}
