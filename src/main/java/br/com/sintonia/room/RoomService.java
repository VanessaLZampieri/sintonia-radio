package br.com.sintonia.room;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.SQLException;

@Service
public class RoomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 5;

    private final RoomRepository roomRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room createRoom() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generateCode();
            Room room = new Room(code, RoomStatus.ACTIVE);
            try {
                return roomRepository.save(room);
            } catch (DataIntegrityViolationException exception) {
                if (!isCodeUniqueViolation(exception)) {
                    throw exception;
                }
            }
        }
        throw new RoomCodeGenerationException(
                "Não foi possível gerar um código de sala disponível após " + MAX_ATTEMPTS + " tentativas.");
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(CODE_ALPHABET.length());
            code.append(CODE_ALPHABET.charAt(index));
        }
        return code.toString();
    }

    private boolean isCodeUniqueViolation(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return "23505".equals(sqlException.getSQLState())
                        && sqlException.getMessage() != null
                        && sqlException.getMessage().contains("uk_rooms_code");
            }
        }
        return false;
    }
}
