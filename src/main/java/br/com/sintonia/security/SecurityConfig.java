package br.com.sintonia.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {

    private final SintoniaOAuth2UserService sintoniaOAuth2UserService;

    public SecurityConfig(SintoniaOAuth2UserService sintoniaOAuth2UserService) {
        this.sintoniaOAuth2UserService = sintoniaOAuth2UserService;
    }

    @Bean
    public AuthenticationFailureHandler oauthFailureHandler() {
        SimpleUrlAuthenticationFailureHandler handler =
                new SimpleUrlAuthenticationFailureHandler("/login?error=oauth");
        handler.setAllowSessionCreation(false);
        return handler;
    }

    @Bean
    public AuthenticationSuccessHandler oauthSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/");
        handler.setAlwaysUseDefaultTargetUrl(true);
        return handler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationFailureHandler oauthFailureHandler,
                                                   AuthenticationSuccessHandler oauthSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**", "/*.js", "/*.css",
                                "/*.svg", "/*.png", "/*.ico", "/favicon.ico").permitAll()
                        .requestMatchers("/login", "/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers("/api/**", "/rooms/**", "/ws/**").authenticated()
                        .anyRequest().permitAll())
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN)))
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(oauthSuccessHandler)
                        .failureHandler(oauthFailureHandler)
                        .withObjectPostProcessor(
                                new ObjectPostProcessor<OAuth2AuthorizationRequestRedirectFilter>() {
                                    @Override
                                    public <O extends OAuth2AuthorizationRequestRedirectFilter> O postProcess(O filter) {
                                        filter.setAuthenticationFailureHandler(oauthFailureHandler);
                                        return filter;
                                    }
                                })
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(sintoniaOAuth2UserService)))
                .csrf(csrf -> csrf.spa())
                .cors(cors -> {
                });

        return http.build();
    }
}
