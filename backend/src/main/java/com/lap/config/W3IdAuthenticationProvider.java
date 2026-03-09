package com.lap.config;

import com.lap.entity.User;
import com.lap.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class W3IdAuthenticationProvider implements OpaqueTokenAuthenticationConverter {

    @Value("${w3id.userinfo-uri}")
    private String userinfoUri;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Authentication convert(String introspectedToken, OAuth2AuthenticatedPrincipal principal) {
        // Get email from introspection result
        String email = principal.getAttribute("sub");

        // If no email from introspection, try userinfo endpoint
        if (email == null || !email.contains("@")) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(introspectedToken);
                ResponseEntity<Map> response = restTemplate.exchange(
                    userinfoUri, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class
                );
                Map<String, Object> userInfo = response.getBody();
                if (userInfo != null) {
                    email = (String) userInfo.getOrDefault("email",
                        userInfo.getOrDefault("sub", email));
                }
            } catch (Exception e) {
                // Fall back to sub from introspection
            }
        }

        if (email == null) {
            return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        }

        final String userEmail = email;

        // Find or create user
        User user = userRepository
            .findByEmail(userEmail)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(userEmail);
                String username = userEmail.contains("@")
                    ? userEmail.substring(0, userEmail.indexOf("@"))
                    : userEmail;
                newUser.setUsername(username);
                newUser.setPassword("W3ID_SSO_USER");
                newUser.setIsActive(true);
                return userRepository.save(newUser);
            });

        return new UsernamePasswordAuthenticationToken(
            user, introspectedToken, user.getAuthorities()
        );
    }
}
