package br.com.sintonia.room;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomStateControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;

    private MockMvc mockMvc;
    private RoomStateService roomStateService;

    @BeforeEach
    void setUp() {
        roomStateService = mock(RoomStateService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomStateController(roomStateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getsRoomState() throws Exception {
        RoomStateResponse response = new RoomStateResponse(
                ROOM_ID, "ABCDEFGH", RoomStatus.ACTIVE, PlaybackMode.TODOS_OS_NAVEGADORES,
                null, null, List.of(), null);
        when(roomStateService.get(ROOM_ID, USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/rooms/{roomId}/state", ROOM_ID).with(auth(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.roomCode").value("ABCDEFGH"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.playbackMode").value("TODOS_OS_NAVEGADORES"))
                .andExpect(jsonPath("$.queue").isEmpty());

        verify(roomStateService).get(ROOM_ID, USER_ID);
    }

    @Test
    void returnsNotFoundWhenRoomDoesNotExist() throws Exception {
        when(roomStateService.get(ROOM_ID, USER_ID)).thenThrow(new RoomNotFoundException("Sala não encontrada."));

        mockMvc.perform(get("/api/rooms/{roomId}/state", ROOM_ID).with(auth(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sala não encontrada."));
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
