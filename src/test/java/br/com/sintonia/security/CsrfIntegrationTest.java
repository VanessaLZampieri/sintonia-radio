package br.com.sintonia.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import org.springframework.security.core.Authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CsrfIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    @Qualifier("oauthSuccessHandler")
    private AuthenticationSuccessHandler oauthSuccessHandler;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void csrfTokenFromCookieIsAcceptedInHeader() throws Exception {
        MvcResult getResult = mockMvc.perform(get("/")).andReturn();

        Cookie csrfCookie = getResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isNotBlank();

        MvcResult postResult = mockMvc
                .perform(post("/rooms")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andReturn();

        assertThat(postResult.getResponse().getStatus()).isNotEqualTo(403);
    }

    @Test
    void invalidCsrfTokenIsRejected() throws Exception {
        MvcResult getResult = mockMvc.perform(get("/")).andReturn();
        Cookie csrfCookie = getResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/rooms")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", "invalid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customLoginPageUsesSpa() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void generatedSpringLoginPageIsNotInstalled() {
        assertThat(springSecurityFilterChain.getFilters("/login"))
                .noneMatch(DefaultLoginPageGeneratingFilter.class::isInstance);
    }

    @Test
    void googleAuthorizationStartsWithoutIntermediateLoginPage() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("https://accounts.google.com/")));
    }

    @Test
    void failedOAuthCallbackReturnsToSpaLogin() throws Exception {
        mockMvc.perform(get("/login/oauth2/code/google")
                        .queryParam("error", "access_denied")
                        .queryParam("state", "invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login?error=oauth"));
    }

    @Test
    void successfulOAuthAlwaysReturnsToApplicationHome() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        oauthSuccessHandler.onAuthenticationSuccess(request, response, mock(Authentication.class));

        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }

    @Test
    void unauthenticatedApiRequestReturnsEmptyUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }
}
