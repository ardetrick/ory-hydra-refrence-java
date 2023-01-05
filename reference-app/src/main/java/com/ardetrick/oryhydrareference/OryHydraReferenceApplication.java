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
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// TODO: revisit and understand why SpringBoot 3 broke the old CSRF code
		http.csrf().disable();
		return http.build();
	}

}
