package br.com.sintonia.room;

import br.com.sintonia.security.SintoniaOAuth2User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomSummaryControllerTest {

    @Test
    void returnsRoomSummary() throws Exception {
        RoomSummaryService service = mock(RoomSummaryService.class);
        RoomSummaryResponse response = new RoomSummaryResponse(
                1L, 24, 4, 3, 6, 11,
                List.of(new RoomSummaryResponse.UserContribution(
                        7L, "Vanessa", null, 8, 5, 2, 4)));
        when(service.get(1L, 7L)).thenReturn(response);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RoomSummaryController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        mockMvc.perform(get("/api/rooms/1/summary").with(auth(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playbackCount").value(24))
                .andExpect(jsonPath("$.participantCount").value(4))
                .andExpect(jsonPath("$.skippedCount").value(3))
                .andExpect(jsonPath("$.autoDjPlaybackCount").value(6))
                .andExpect(jsonPath("$.skipVoteCount").value(11))
                .andExpect(jsonPath("$.contributions[0].displayName").value("Vanessa"))
                .andExpect(jsonPath("$.contributions[0].addedCount").value(8));
        SecurityContextHolder.clearContext();
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
