package com.ardetrick.oryhydrareference;

import com.ardetrick.testcontainers.OryHydraContainer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

/**
 * Development-time entry point: starts the app together with a real Ory Hydra container and a
 * seeded demo client, so running locally is one command with nothing to install but Docker.
 *
 * <p>Run {@code ./gradlew bootTestRun}, then open <a
 * href="http://localhost:8080">http://localhost:8080</a> and log in with {@code foo@bar.com} /
 * {@code password}.
 */
public class TestOryHydraReferenceApplication {

  public static void main(String[] args) {
    SpringApplication.from(OryHydraReferenceApplication::main)
        .with(LocalHydraConfiguration.class)
        .run(args);
  }

  @Slf4j
  @TestConfiguration(proxyBeanMethods = false)
  static class LocalHydraConfiguration {

    // End the startup logs with the link to click — the convention of one-command dev servers.
    // Dev-time only: the production app cannot know its public URL, but this configuration
    // already pins every port, so it can.
    @Bean
    ApplicationListener<ApplicationReadyEvent> readyMessage() {
      return event ->
          log.info(
              "Demo ready — open http://localhost:8080 and log in with foo@bar.com / password");
    }

    // Hydra's host ports are pinned to the image defaults here, so Hydra's dev-mode issuer
    // (http://localhost:4444), the app's default admin base path (http://localhost:4445), and
    // the hardcoded links on /demo all line up with no configuration. Unlike the functional
    // tests, a dev-time run has no parallelism to worry about, so fixed ports are fine. The
    // demo client's secret matches the one the /demo page's curl snippet assumes.
    @Bean(destroyMethod = "stop")
    OryHydraContainer oryHydraContainer() {
      OryHydraContainer hydra =
          OryHydraContainer.builder()
              .urlsLogin("http://localhost:8080/login")
              .urlsConsent("http://localhost:8080/consent")
              .urlsLogout("http://localhost:8080/logout")
              // Without an explicit issuer, Hydra derives it from its listen address
              // (0.0.0.0:4444), and browsers refuse to follow the issuer-based redirects the
              // flow makes mid-way (Safari outright blocks 0.0.0.0).
              .urlsSelfIssuer("http://localhost:4444")
              .client(
                  client ->
                      client
                          .clientId("demo-client")
                          .clientSecret("omit-for-random-secret-1")
                          // The app's own demo callback page, so the flow completes in the
                          // browser end to end.
                          .redirectUris("http://localhost:8080/callback")
                          .grantTypes("authorization_code", "refresh_token")
                          .responseTypes("code", "id_token")
                          .scope("openid", "offline", "offline_access", "profile")
                          .put("client_name", "Demo Client"))
              .build();
      hydra.setPortBindings(List.of("4444:4444", "4445:4445"));
      hydra.start();
      return hydra;
    }
  }
}
