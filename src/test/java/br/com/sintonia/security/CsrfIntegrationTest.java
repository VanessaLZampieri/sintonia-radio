package br.com.sintonia.security;

import br.com.sintonia.web.SpaForwardController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CsrfIntegrationTest.TestApplication.class)
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
    void authenticatedLogoutWithoutCsrfIsRejectedAndKeepsSession() throws Exception {
        MockHttpSession session = authenticatedSession();

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());

        assertThat(session.isInvalid()).isFalse();
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isNotNull();
    }

    @Test
    void authenticatedLogoutWithCookieCsrfInvalidatesSessionAndClearsToken() throws Exception {
        MockHttpSession session = authenticatedSession();
        Cookie csrfCookie = csrfCookie(session);

        mockMvc.perform(post("/logout")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void authenticatedApiIsUnauthorizedAfterLogout() throws Exception {
        MockHttpSession session = authenticatedSession();
        String sessionId = session.getId();
        Cookie csrfCookie = csrfCookie(session);

        mockMvc.perform(post("/logout")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me")
                        .cookie(new Cookie("JSESSIONID", sessionId)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
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

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession(context.getServletContext());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new TestingAuthenticationToken("user", null, "ROLE_USER"));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    private Cookie csrfCookie(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        return csrfCookie;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
    })
    @Import({SecurityConfig.class, SpaForwardController.class, TestEndpoints.class})
    static class TestApplication {

        @Bean
        SintoniaOAuth2UserService sintoniaOAuth2UserService() {
            return mock(SintoniaOAuth2UserService.class);
        }
    }

    @RestController
    static class TestEndpoints {

        @GetMapping("/")
        ResponseEntity<Void> home() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/rooms")
        ResponseEntity<Void> createRoom() {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        @GetMapping("/api/me")
        ResponseEntity<Void> me() {
            return ResponseEntity.ok().build();
        }
    }
}
