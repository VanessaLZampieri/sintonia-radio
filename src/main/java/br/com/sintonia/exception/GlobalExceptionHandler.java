package br.com.sintonia.exception;

import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomFullException;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.room.UserNotInRoomException;
import br.com.sintonia.queue.SongAlreadyInQueueException;
import br.com.sintonia.queue.QueueItemNotFoundException;
import br.com.sintonia.queue.QueueLimitExceededException;
import br.com.sintonia.song.SongNotFoundException;
import br.com.sintonia.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRoomNotFound(RoomNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RoomClosedException.class)
    public ResponseEntity<Map<String, String>> handleRoomClosed(RoomClosedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RoomFullException.class)
    public ResponseEntity<Map<String, String>> handleRoomFull(RoomFullException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleRestClient(RestClientException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Serviço do YouTube indisponível no momento."));
    }

    @ExceptionHandler(SongNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSongNotFound(SongNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(SongAlreadyInQueueException.class)
    public ResponseEntity<Map<String, String>> handleSongAlreadyInQueue(SongAlreadyInQueueException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(QueueLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleQueueLimitExceeded(QueueLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(UserNotInRoomException.class)
    public ResponseEntity<Map<String, String>> handleUserNotInRoom(UserNotInRoomException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(QueueItemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleQueueItemNotFound(QueueItemNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }
}