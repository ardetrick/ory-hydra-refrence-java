package com.ardetrick.oryhydrareference.testcontainers;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.net.URI;
import java.time.Duration;

public class OryHydraDockerComposeContainer<SELF extends OryHydraDockerComposeContainer<SELF>> extends DockerComposeContainer<SELF> {

    static final int HYDRA_ADMIN_PORT = 4445;
    static final int HYDRA_PUBLIC_PORT = 4444;
    static final String SERVICE_NAME = "hydra_1";

    public static OryHydraDockerComposeContainer<?> start(int port) {
        var compose = new OryHydraDockerComposeContainer<>(port);
        compose.start();

        return compose;
    }

    OryHydraDockerComposeContainer(int port) {
        this(new File("src/test/resources/hydra-docker-compose.yml"), port);
    }

    OryHydraDockerComposeContainer(File composeFile, int port) {
        super(composeFile);
        this.withEnv("URLS_LOGIN", "http://localhost:" + port + "/login");
        this.withEnv("URLS_CONSENT", "http://localhost:" + port + "/consent");
        this.withEnv("URLS_SELF_ISSUER", "http://localhost:" + port + "/integration-test-public-proxy");
        this.withExposedService(SERVICE_NAME, HYDRA_ADMIN_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));
        this.withExposedService(SERVICE_NAME, HYDRA_PUBLIC_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));
    }

    public String adminBaseUriString() {
        return baseUriString(HYDRA_ADMIN_PORT);
    }

    public String publicBaseUriString() {
        return baseUriString(HYDRA_PUBLIC_PORT);
    }

    private String baseUriString(int port) {
        return "http://" +
                getServiceHost(SERVICE_NAME, port) +
                ":" +
                getServicePort(SERVICE_NAME, port);
    }

    public URI getOAuth2AuthUri() {
        return URI.create(publicBaseUriString() + "/oauth2/auth");
    }

    public URI getPublicJwksUri() {
        return URI.create(publicBaseUriString() + "/.well-known/jwks.json");
    }

}
