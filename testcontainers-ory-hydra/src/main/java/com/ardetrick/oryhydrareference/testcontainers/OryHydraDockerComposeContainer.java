package com.ardetrick.oryhydrareference.testcontainers;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class OryHydraDockerComposeContainer<SELF extends OryHydraDockerComposeContainer<SELF>> extends DockerComposeContainer<SELF> {

    static final int HYDRA_ADMIN_PORT = 4445;
    static final int HYDRA_PUBLIC_PORT = 4444;
    static final String SERVICE_NAME = "hydra_1";
    static final WaitStrategy DEFAULT_WAIT_STRATEGY = Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10));

    public static OryHydraDockerComposeContainer.Builder builder() {
        return new Builder();
    }

    private OryHydraDockerComposeContainer(
            File composeFile,
            Map<String, String> env
    ) {
        super(composeFile);
        this.withEnv(env);
        this.withExposedService(SERVICE_NAME, HYDRA_ADMIN_PORT, DEFAULT_WAIT_STRATEGY);
        this.withExposedService(SERVICE_NAME, HYDRA_PUBLIC_PORT, DEFAULT_WAIT_STRATEGY);
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

    public static class Builder {

        File dockerComposeFile;
        Map<String, String> env = new HashMap<>();

        public Builder dockerComposeFile(File file) {
            this.dockerComposeFile = file;
            return this;
        }

        public Builder urlsLogin(String s) {
            this.env.put("URLS_LOGIN", s);
            return this;
        }

        public Builder urlsConsent(String s) {
            this.env.put("URLS_CONSENT", s);
            return this;
        }

        public Builder urlsSelfIssuer(String s) {
            this.env.put("URLS_SELF_ISSUER", s);
            return this;
        }

        public OryHydraDockerComposeContainer<?> start() {
            if (dockerComposeFile == null) {
                throw new IllegalStateException("file must be non-null");
            }

            var compose = new OryHydraDockerComposeContainer<>(dockerComposeFile, env);
            compose.start();

            return compose;
        }

    }

}
