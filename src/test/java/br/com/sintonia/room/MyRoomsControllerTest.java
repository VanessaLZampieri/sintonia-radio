package br.com.sintonia.room;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyRoomsControllerTest {

    private static final Long USER_ID = 100L;

    private MockMvc mockMvc;
    private RoomDiscoveryService roomDiscoveryService;

    @BeforeEach
    void setUp() {
        roomDiscoveryService = mock(RoomDiscoveryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MyRoomsController(roomDiscoveryService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsMyRooms() throws Exception {
        when(roomDiscoveryService.listUserRooms(eq(USER_ID))).thenReturn(List.of(
                new UserRoomResponse(1L, "Sala A", "CODE1", RoomStatus.ACTIVE, true, 3L),
                new UserRoomResponse(2L, "Sala B", "CODE2", RoomStatus.CLOSED, false, 0L)));

        mockMvc.perform(get("/api/me/rooms").with(auth(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(1))
                .andExpect(jsonPath("$[0].canEnter").value(true))
                .andExpect(jsonPath("$[1].roomId").value(2))
                .andExpect(jsonPath("$[1].status").value("CLOSED"))
                .andExpect(jsonPath("$[1].canEnter").value(false));
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
