package com.ardetrick.oryhydrareference.hydra;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import sh.ory.hydra.ApiException;
import sh.ory.hydra.Configuration;
import sh.ory.hydra.api.OAuth2Api;
import sh.ory.hydra.model.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HydraAdminClient {

    @NonNull OAuth2Api oAuth2Api;

    HydraAdminClient(@NonNull final HydraAdminClient.Properties properties) {
        val apiClient = Configuration.getDefaultApiClient()
                        .setBasePath(properties.getBasePath());
        oAuth2Api = new OAuth2Api(apiClient);
    }

    public List<OAuth2Client> listOAuth2Clients() {
        try {
            return oAuth2Api.listOAuth2Clients(1000L, null, null, null);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <a href="https://github.com/ory/hydra-client-java/blob/master/docs/OAuth2Api.md#getoauth2loginrequest">...</a>
     */
    public Optional<OAuth2LoginRequest> getLoginRequest(@NonNull String loginChallenge) {
        try {
            return Optional.of(oAuth2Api.getOAuth2LoginRequest(loginChallenge));
        } catch (ApiException e) {
            return switch (e.getCode()) {
                case 410 -> Optional.empty(); // requestWasHandledResponse
                case 400, 404 -> Optional.empty(); // jsonError
                case 500 -> Optional.empty(); // jsonError
                default -> throw new RuntimeException("unhandled code: " + e.getCode(), e);
            };
        }
    }

    public OAuth2RedirectTo acceptLoginRequest(
            @NonNull String loginChallenge,
            @NonNull String loginEmail
    ) {
        val acceptLoginRequest = new AcceptOAuth2LoginRequest();
        // Subject is an alias for user ID. A subject can be a random string, a UUID, an email address, ...
        acceptLoginRequest.subject(loginEmail);
        try {
            return oAuth2Api.acceptOAuth2LoginRequest(loginChallenge, acceptLoginRequest);
        } catch (ApiException e) {
            switch (e.getCode()) {
                case 400, 401, 404, 500 -> throw new RuntimeException("code: " + e.getCode(), e); // jsonError
                default -> throw new RuntimeException("unhandled code: " + e.getCode(), e);
            }
        }
    }

    public OAuth2ConsentRequest getConsentRequest(@NonNull String consentChallenge) {
        try {
            return oAuth2Api.getOAuth2ConsentRequest(consentChallenge);
        } catch (ApiException e) {
            switch (e.getCode()) {
                case 400, 404 -> throw new RuntimeException("code: " + e.getCode(), e); // jsonError
                default -> throw new RuntimeException("unhandled code: " + e.getCode(), e);
            }
        }
    }

    public OAuth2RedirectTo acceptConsentRequest(
            @NonNull AcceptConsentRequest acceptConsentRequest
    ) {
        val acceptOAuth2ConsentRequest = OryHydraRequestMapper.map(acceptConsentRequest);

        try {
            return oAuth2Api.acceptOAuth2ConsentRequest(
                    acceptConsentRequest.consentChallenge(),
                    acceptOAuth2ConsentRequest
            );
        } catch (ApiException e) {
            switch (e.getCode()) {
                case 404, 500 -> throw new RuntimeException("code: " + e.getCode(), e); // jsonError
                default -> throw new RuntimeException("unhandled code: " + e.getCode(), e);
            }
        }
    }

    @Data
    @org.springframework.context.annotation.Configuration
    @ConfigurationProperties("reference-app.hydra")
    public static class Properties {

        String basePath = "http://localhost:4445";

    }

}
