package br.com.sintonia.playback;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public SkipVoteResponse vote(@PathVariable Long roomId, @RequestBody SkipVoteRequest request) {
        if (request.userId() == null || request.userId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId é obrigatório.");
        }
        return skipVoteService.vote(roomId, request.userId());
    }

    @GetMapping("/{roomId}/skip-votes")
    public SkipVoteResponse current(@PathVariable Long roomId) {
        return skipVoteService.current(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não há playback em andamento."));
    }
}
