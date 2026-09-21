package br.com.sintonia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final SintoniaOAuth2UserService sintoniaOAuth2UserService;

    public SecurityConfig(SintoniaOAuth2UserService sintoniaOAuth2UserService) {
        this.sintoniaOAuth2UserService = sintoniaOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**", "/*.js", "/*.css",
                                "/*.svg", "/*.png", "/*.ico", "/favicon.ico").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                        .requestMatchers("/api/**", "/rooms/**", "/ws/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(sintoniaOAuth2UserService)))
                .csrf(csrf -> csrf.spa())
                .cors(cors -> {
                });

        return http.build();
    }
}
