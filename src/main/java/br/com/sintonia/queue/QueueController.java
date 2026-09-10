package br.com.sintonia.queue;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/{roomId}/queue")
    public List<QueueItemResponse> findQueue(@PathVariable Long roomId) {
        List<QueueItem> items = queueService.findQueue(roomId);
        return items.stream().map(QueueItemResponse::from).toList();
    }

    @DeleteMapping("/{roomId}/queue/{queueItemId}")
    @ResponseStatus(HttpStatus.OK)
    public void remove(@PathVariable Long roomId,
                           @PathVariable Long queueItemId,
                           @RequestBody RemoveQueueItemRequest request) {
        if (request.userId() == null || request.userId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId é obrigatório.");
        }
        queueService.remove(roomId, queueItemId, request.userId());
    }

    @PostMapping("/{roomId}/queue")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueItemResponse add(@PathVariable Long roomId,
                                  @RequestBody AddQueueItemRequest request) {
        if (request.songId() == null || request.songId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "songId é obrigatório.");
        }
        if (request.userId() == null || request.userId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId é obrigatório.");
        }
        QueueItem item = queueService.add(roomId, request.songId(), request.userId());
        return QueueItemResponse.from(item);
    }
}