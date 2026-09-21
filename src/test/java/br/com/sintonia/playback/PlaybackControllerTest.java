package br.com.sintonia.playback;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaybackControllerTest {

    private static final Long USER_ID = 100L;
    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;
    private PlaybackService playbackService;

    @BeforeEach
    void setUp() {
        playbackService = mock(PlaybackService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlaybackController(playbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void finishesPlayback() throws Exception {
        mockMvc.perform(post("/api/playbacks/{playbackId}/finish", 1L)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"" + SESSION_ID + "\"}"))
                .andExpect(status().isOk());

        verify(playbackService).finish(1L, USER_ID, SESSION_ID);
    }

    @Test
    void errorsPlayback() throws Exception {
        mockMvc.perform(post("/api/playbacks/{playbackId}/error", 1L)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"" + SESSION_ID + "\"}"))
                .andExpect(status().isOk());

        verify(playbackService).error(1L, USER_ID, SESSION_ID);
    }

    @Test
    void pausesPlayback() throws Exception {
        mockMvc.perform(post("/api/playbacks/{playbackId}/pause", 1L)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"" + SESSION_ID + "\"}"))
                .andExpect(status().isOk());

        verify(playbackService).pause(1L, USER_ID, SESSION_ID);
    }

    @Test
    void resumesPlayback() throws Exception {
        mockMvc.perform(post("/api/playbacks/{playbackId}/resume", 1L)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"" + SESSION_ID + "\"}"))
                .andExpect(status().isOk());

        verify(playbackService).resume(1L, USER_ID, SESSION_ID);
    }

    @Test
    void returnsConflictWhenFinishingNonPlayingPlayback() throws Exception {
        when(playbackService.finish(1L, USER_ID, SESSION_ID))
                .thenThrow(new PlaybackNotPlayingException("O playback não está em andamento."));

        mockMvc.perform(post("/api/playbacks/{playbackId}/finish", 1L)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"" + SESSION_ID + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundWhenPlaybackDoesNotExist() throws Exception {
        when(playbackService.error(1L, USER_ID, SESSION_ID))
                .thenThrow(new PlaybackNotFoundException("Playback não encontrado."));

        mockMvc.perform(post("/api/playbacks/{playbackId}/error", 1L)
                        .with(auth(USER_ID))
                        .contentType("application/json")
                        .content("{\"clientSessionId\":\"" + SESSION_ID + "\"}"))
                .andExpect(status().isNotFound());
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
