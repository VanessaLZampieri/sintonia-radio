package br.com.sintonia.playback;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playbacks")
public class PlaybackController {

    private final PlaybackService playbackService;

    public PlaybackController(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @PostMapping("/{playbackId}/finish")
    @ResponseStatus(HttpStatus.OK)
    public void finish(@PathVariable Long playbackId,
                       @RequestBody(required = false) PlaybackCommandRequest request,
                       @AuthenticationPrincipal SintoniaOAuth2User principal) {
        playbackService.finish(playbackId, principal.getUserId(), clientSessionId(request));
    }

    @PostMapping("/{playbackId}/error")
    @ResponseStatus(HttpStatus.OK)
    public void error(@PathVariable Long playbackId,
                      @RequestBody(required = false) PlaybackCommandRequest request,
                      @AuthenticationPrincipal SintoniaOAuth2User principal) {
        playbackService.error(playbackId, principal.getUserId(), clientSessionId(request));
    }

    @PostMapping("/{playbackId}/pause")
    @ResponseStatus(HttpStatus.OK)
    public void pause(@PathVariable Long playbackId,
                      @RequestBody(required = false) PlaybackCommandRequest request,
                      @AuthenticationPrincipal SintoniaOAuth2User principal) {
        playbackService.pause(playbackId, principal.getUserId(), clientSessionId(request));
    }

    @PostMapping("/{playbackId}/resume")
    @ResponseStatus(HttpStatus.OK)
    public void resume(@PathVariable Long playbackId,
                       @RequestBody(required = false) PlaybackCommandRequest request,
                       @AuthenticationPrincipal SintoniaOAuth2User principal) {
        playbackService.resume(playbackId, principal.getUserId(), clientSessionId(request));
    }

    private String clientSessionId(PlaybackCommandRequest request) {
        return request == null ? null : request.clientSessionId();
    }
}
