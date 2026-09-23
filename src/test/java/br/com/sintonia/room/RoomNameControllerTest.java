package br.com.sintonia.room;

import br.com.sintonia.exception.GlobalExceptionHandler;
import br.com.sintonia.security.SintoniaOAuth2User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomNameControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 100L;

    private MockMvc mockMvc;
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = mock(RoomService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomNameController(roomService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void renamesRoom() throws Exception {
        RoomResponse response = new RoomResponse(
                ROOM_ID, "ABCDEFGH", "Nova Sala", RoomStatus.ACTIVE, Instant.parse("2026-09-22T10:00:00Z"));
        when(roomService.rename(eq(ROOM_ID), eq("Nova Sala"), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(patch("/api/rooms/{roomId}", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nova Sala\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("ABCDEFGH"))
                .andExpect(jsonPath("$.name").value("Nova Sala"));
    }

    @Test
    void mapsInvalidNameToBadRequest() throws Exception {
        when(roomService.rename(eq(ROOM_ID), eq("ab"), eq(USER_ID)))
                .thenThrow(new InvalidRoomNameException("O nome da sala deve ter entre 3 e 40 caracteres."));

        mockMvc.perform(patch("/api/rooms/{roomId}", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O nome da sala deve ter entre 3 e 40 caracteres."));
    }

    @Test
    void mapsClosedRoomToConflict() throws Exception {
        when(roomService.rename(eq(ROOM_ID), eq("Nova Sala"), eq(USER_ID)))
                .thenThrow(new RoomClosedException("Esta sala foi encerrada."));

        mockMvc.perform(patch("/api/rooms/{roomId}", ROOM_ID)
                        .with(auth(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nova Sala\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta sala foi encerrada."));
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
