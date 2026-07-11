package com.ardetrick.oryhydrareference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
public class OryHydraReferenceApplication {

  public static void main(String[] args) {
    SpringApplication.run(OryHydraReferenceApplication.class, args);
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) {
    // Don't do this in production. This was removed to simplify this reference implementation.
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        // Spring Security's built-in LogoutFilter intercepts /logout before it can reach any
        // controller (with CSRF disabled, even on GET) and redirects to its own /login?logout.
        // This app's /logout is Hydra's logout challenge endpoint, not an app-session logout,
        // so the filter must be disabled for LogoutController to receive the request.
        .logout(AbstractHttpConfigurer::disable);
    return http.build();
  }
}
