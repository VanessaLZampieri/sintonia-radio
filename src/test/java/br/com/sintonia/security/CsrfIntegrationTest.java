package br.com.sintonia.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CsrfIntegrationTest {

    @Autowired
    private WebApplicationContext context;

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
}
