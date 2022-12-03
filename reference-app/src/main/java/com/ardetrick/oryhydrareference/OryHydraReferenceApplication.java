package com.ardetrick.oryhydrareference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class OryHydraReferenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OryHydraReferenceApplication.class, args);
	}

	@ConfigurationProperties("reference-app")
	@Configuration
	static class Config {

		String oryHydraPublicUri;

	}

}
