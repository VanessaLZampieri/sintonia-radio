package br.com.sintonia.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final int MIN_DISPLAY_NAME_LENGTH = 2;
    private static final int MAX_DISPLAY_NAME_LENGTH = 20;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        return MeResponse.from(findUser(userId));
    }

    @Transactional
    public MeResponse updateDisplayName(Long userId, String displayName) {
        String trimmed = displayName == null ? "" : displayName.trim();
        if (trimmed.length() < MIN_DISPLAY_NAME_LENGTH || trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new InvalidDisplayNameException(
                    "O nome de exibição deve ter entre " + MIN_DISPLAY_NAME_LENGTH
                            + " e " + MAX_DISPLAY_NAME_LENGTH + " caracteres.");
        }

        User user = findUser(userId);
        user.updateDisplayName(trimmed);
        return MeResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
    }
}
