package br.com.sintonia.user;

import br.com.sintonia.exception.GlobalExceptionHandler;
import br.com.sintonia.security.SintoniaOAuth2User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeControllerTest {

    private static final Long USER_ID = 10L;

    private MockMvc mockMvc;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MeController(userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCurrentUser() throws Exception {
        User user = new User("google-123", "João Silva", "joao@example.com", "https://img/joao.jpg");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/me").with(auth(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.avatarUrl").value("https://img/joao.jpg"));
    }

    @Test
    void returnsNotFoundWhenUserMissing() throws Exception {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/me").with(auth(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário não encontrado."));
    }

    @Test
    void returnsUnauthorizedWhenPrincipalMissing() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor auth(Long userId) {
        OidcUser delegate = mock(OidcUser.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new SintoniaOAuth2User(delegate, userId), null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return request -> {
            SecurityContextHolder.setContext(context);
            return request;
        };
    }
}
