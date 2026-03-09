package com.lap.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private W3IdAuthenticationProvider w3IdAuthenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz ->
                authz
                    .requestMatchers("/api/auth/me").authenticated()
                    .requestMatchers("/api/items/all").permitAll()
                    .requestMatchers("/api/files/download/**").permitAll()
                    .requestMatchers("/api/files/health").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/favicon.ico").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.opaqueToken(opaque ->
                    opaque.authenticationConverter(w3IdAuthenticationProvider)
                )
            );

        return http.build();
    }
}
