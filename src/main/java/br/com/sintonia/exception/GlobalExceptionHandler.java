package br.com.sintonia.exception;

import br.com.sintonia.room.RoomClosedException;
import br.com.sintonia.room.RoomFullException;
import br.com.sintonia.room.RoomNotFoundException;
import br.com.sintonia.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
