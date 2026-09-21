package br.com.sintonia.user;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void createsUserWithAllFields() {
        User user = new User("google-123", "João", "joao@example.com", "https://img/joao.jpg");

        assertThat(user.getGoogleId()).isEqualTo("google-123");
        assertThat(user.getName()).isEqualTo("João");
        assertThat(user.getEmail()).isEqualTo("joao@example.com");
        assertThat(user.getAvatarUrl()).isEqualTo("https://img/joao.jpg");
    }

    @Test
    void allowsNullAvatar() {
        User user = new User("google-123", "João", "joao@example.com", null);

        assertThat(user.getAvatarUrl()).isNull();
    }

    @Test
    void rejectsNullGoogleId() {
        assertThatThrownBy(() -> new User(null, "João", "joao@example.com", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new User("google-123", null, "joao@example.com", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullEmail() {
        assertThatThrownBy(() -> new User("google-123", "João", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updatesProfile() {
        User user = new User("google-123", "João", "joao@example.com", "https://img/joao.jpg");
        user.updateProfile("João Atualizado", "novo@example.com", "https://img/novo.jpg");

        assertThat(user.getName()).isEqualTo("João Atualizado");
        assertThat(user.getEmail()).isEqualTo("novo@example.com");
        assertThat(user.getAvatarUrl()).isEqualTo("https://img/novo.jpg");
    }

    @Test
    void updateProfileRejectsNullName() {
        User user = new User("google-123", "João", "joao@example.com", null);

        assertThatThrownBy(() -> user.updateProfile(null, "novo@example.com", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void exposesIdAndCreatedAt() {
        User user = new User("google-123", "João", "joao@example.com", null);
        ReflectionTestUtils.setField(user, "id", 10L);

        assertThat(user.getId()).isEqualTo(10L);
        assertThat(user.getCreatedAt()).isNull();
    }
}
