package com.ardetrick.oryhydrareference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@SpringBootApplication
public class OryHydraReferenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OryHydraReferenceApplication.class, args);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// Don't use this configuration in production. Cookies should be set with http only, but is beyond
		// the scope of reference implementation at the moment. Alternatively, this could be moved to the integration
		// test config or under a dev only toggle.
		http.csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
		return http.build();
	}

}
