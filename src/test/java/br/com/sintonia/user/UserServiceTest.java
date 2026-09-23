package br.com.sintonia.user;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final Long USER_ID = 10L;

    @Test
    void getMeReturnsUser() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User("google-123", "João Silva", "joao@example.com", "https://img/joao.jpg");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserService service = new UserService(repository);

        MeResponse response = service.getMe(USER_ID);

        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.name()).isEqualTo("João Silva");
        assertThat(response.displayName()).isEqualTo("João");
        assertThat(response.email()).isEqualTo("joao@example.com");
    }

    @Test
    void updateDisplayNameTrimsAndPersists() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User("google-123", "João Silva", "joao@example.com", null);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(repository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserService service = new UserService(repository);

        MeResponse response = service.updateDisplayName(USER_ID, "  Ana Beatriz  ");

        assertThat(user.getDisplayName()).isEqualTo("Ana Beatriz");
        assertThat(response.displayName()).isEqualTo("Ana Beatriz");
    }

    @Test
    void updateDisplayNameRejectsTooShort() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository);

        assertThatThrownBy(() -> service.updateDisplayName(USER_ID, "A"))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void updateDisplayNameRejectsTooLong() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository);

        assertThatThrownBy(() -> service.updateDisplayName(USER_ID, "A".repeat(21)))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void updateDisplayNameRejectsBlank() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository);

        assertThatThrownBy(() -> service.updateDisplayName(USER_ID, "   "))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void updateDisplayNameRejectsNull() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository);

        assertThatThrownBy(() -> service.updateDisplayName(USER_ID, null))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void updateDisplayNameThrowsWhenUserMissing() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        UserService service = new UserService(repository);

        assertThatThrownBy(() -> service.updateDisplayName(USER_ID, "Ana"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
