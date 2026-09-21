package br.com.sintonia.security;

import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SintoniaOAuth2UserServiceTest {

    @Test
    void createsUserWhenNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        OAuth2UserService<OidcUserRequest, OidcUser> delegate = mock(OAuth2UserService.class);
        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUser oidcUser = mock(OidcUser.class);

        when(oidcUser.getSubject()).thenReturn("google-123");
        when(oidcUser.getAttribute("name")).thenReturn("João Silva");
        when(oidcUser.getAttribute("email")).thenReturn("joao@example.com");
        when(oidcUser.getAttribute("picture")).thenReturn("https://img/joao.jpg");
        when(delegate.loadUser(request)).thenReturn(oidcUser);
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());

        User saved = new User("google-123", "João Silva", "joao@example.com", "https://img/joao.jpg");
        ReflectionTestUtils.setField(saved, "id", 10L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        SintoniaOAuth2UserService service = new SintoniaOAuth2UserService(userRepository, delegate);

        OidcUser result = service.loadUser(request);

        assertThat(result).isInstanceOf(SintoniaOAuth2User.class);
        assertThat(((SintoniaOAuth2User) result).getUserId()).isEqualTo(10L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updatesExistingUser() {
        UserRepository userRepository = mock(UserRepository.class);
        OAuth2UserService<OidcUserRequest, OidcUser> delegate = mock(OAuth2UserService.class);
        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUser oidcUser = mock(OidcUser.class);

        when(oidcUser.getSubject()).thenReturn("google-123");
        when(oidcUser.getAttribute("name")).thenReturn("João Atualizado");
        when(oidcUser.getAttribute("email")).thenReturn("novo@example.com");
        when(oidcUser.getAttribute("picture")).thenReturn("https://img/novo.jpg");
        when(delegate.loadUser(request)).thenReturn(oidcUser);

        User existing = new User("google-123", "João Silva", "joao@example.com", "https://img/joao.jpg");
        ReflectionTestUtils.setField(existing, "id", 10L);
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));

        SintoniaOAuth2UserService service = new SintoniaOAuth2UserService(userRepository, delegate);

        OidcUser result = service.loadUser(request);

        assertThat(result).isInstanceOf(SintoniaOAuth2User.class);
        assertThat(((SintoniaOAuth2User) result).getUserId()).isEqualTo(10L);
        assertThat(existing.getName()).isEqualTo("João Atualizado");
        assertThat(existing.getEmail()).isEqualTo("novo@example.com");
        assertThat(existing.getAvatarUrl()).isEqualTo("https://img/novo.jpg");
    }
}
