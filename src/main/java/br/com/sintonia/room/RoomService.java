package br.com.sintonia.room;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.SQLException;

@Service
public class RoomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 40;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomService(RoomRepository roomRepository, RoomMemberRepository roomMemberRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    public Room createRoom(String name) {
        String trimmed = validateName(name);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generateCode();
            Room room = new Room(trimmed, code, RoomStatus.ACTIVE);
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

    @Transactional
    public RoomResponse rename(Long roomId, String name, Long userId) {
        String trimmed = validateName(name);

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada."));

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomClosedException("Esta sala foi encerrada.");
        }

        if (!roomMemberRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)) {
            throw new UserNotInRoomException("Usuário não está na sala.");
        }

        room.rename(trimmed);
        return RoomResponse.from(room);
    }

    private String validateName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidRoomNameException(
                    "O nome da sala deve ter entre " + MIN_NAME_LENGTH + " e " + MAX_NAME_LENGTH + " caracteres.");
        }
        return trimmed;
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
