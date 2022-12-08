package com.ardetrick.oryhydrareference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
public class OryHydraReferenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OryHydraReferenceApplication.class, args);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// Applies a default CSRF configuration.
		// https://docs.spring.io/spring-security/reference/features/exploits/csrf.html
		// https://docs.spring.io/spring-security/site/docs/5.0.x/reference/html/csrf.html
		return http.build();
	}

}
