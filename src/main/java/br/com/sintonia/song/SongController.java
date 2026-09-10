package br.com.sintonia.song;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private static final int MAX_RESULTS_LIMIT = 25;

    private final YouTubeSongService youTubeSongService;

    public SongController(YouTubeSongService youTubeSongService) {
        this.youTubeSongService = youTubeSongService;
    }

    @GetMapping("/search")
    public List<SongSearchItemResponse> search(
            @RequestParam("q") String query,
            @RequestParam(value = "maxResults", defaultValue = "10") int maxResults) {
        if (query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O parâmetro 'q' é obrigatório.");
        }
        if (maxResults < 1 || maxResults > MAX_RESULTS_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'maxResults' deve estar entre 1 e " + MAX_RESULTS_LIMIT + ".");
        }
        return youTubeSongService.search(query, maxResults).items().stream()
                .map(SongSearchItemResponse::from)
                .toList();
    }

    @GetMapping("/{youtubeVideoId}")
    public SongResponse select(@PathVariable String youtubeVideoId) {
        return youTubeSongService.select(youtubeVideoId)
                .map(SongResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vídeo não encontrado."));
    }
}
