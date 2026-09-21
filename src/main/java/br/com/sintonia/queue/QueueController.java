package br.com.sintonia.queue;

import br.com.sintonia.playback.PlaybackService;
import br.com.sintonia.security.SintoniaOAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class QueueController {

    private final QueueService queueService;
    private final PlaybackService playbackService;

    public QueueController(QueueService queueService, PlaybackService playbackService) {
        this.queueService = queueService;
        this.playbackService = playbackService;
    }

    @GetMapping("/{roomId}/queue")
    public List<QueueItemResponse> findQueue(@PathVariable Long roomId) {
        return queueService.findQueue(roomId).stream().map(QueueItemResponse::from).toList();
    }

    @DeleteMapping("/{roomId}/queue/{queueItemId}")
    @ResponseStatus(HttpStatus.OK)
    public void remove(@PathVariable Long roomId,
                       @PathVariable Long queueItemId,
                       @AuthenticationPrincipal SintoniaOAuth2User principal) {
        queueService.remove(roomId, queueItemId, principal.getUserId());
    }

    @PostMapping("/{roomId}/queue")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueItemResponse add(@PathVariable Long roomId,
                                 @RequestBody AddQueueItemRequest request,
                                 @AuthenticationPrincipal SintoniaOAuth2User principal) {
        if (request.songId() == null || request.songId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "songId é obrigatório.");
        }
        QueueItem item = queueService.add(roomId, request.songId(), principal.getUserId());
        playbackService.ensurePlayback(roomId);
        return QueueItemResponse.from(item);
    }
}
