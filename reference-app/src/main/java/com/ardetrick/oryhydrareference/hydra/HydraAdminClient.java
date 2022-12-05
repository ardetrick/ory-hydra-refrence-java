package com.ardetrick.oryhydrareference.hydra;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import sh.ory.hydra.ApiException;
import sh.ory.hydra.Configuration;
import sh.ory.hydra.api.OAuth2Api;
import sh.ory.hydra.model.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HydraAdminClient {

    private static final Long DEFAULT_SESSION_EXPIRATION_IN_SECONDS = 3600L;

    @NonNull OAuth2Api oAuth2Api;

    HydraAdminClient() {
        val defaultClient = Configuration.getDefaultApiClient()
                        .setBasePath("http://localhost");
        oAuth2Api = new OAuth2Api(defaultClient);
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
            @NonNull String consentChallenge,
            boolean remember,
            @NonNull List<String> scopes,
            @NonNull OAuth2ConsentRequest consentRequest
    ) {
        val acceptConsentRequest = new AcceptOAuth2ConsentRequest();
        acceptConsentRequest.setGrantScope(scopes);
        acceptConsentRequest.setGrantAccessTokenAudience(consentRequest.getRequestedAccessTokenAudience());
        acceptConsentRequest.remember(remember);
        acceptConsentRequest.rememberFor(DEFAULT_SESSION_EXPIRATION_IN_SECONDS);

        val acceptOAuth2ConsentRequestSession = new AcceptOAuth2ConsentRequestSession();

        // An example of setting custom claims.
        record ExampleCustomClaims(String exampleCustomClaimKey) {}
        acceptOAuth2ConsentRequestSession.setIdToken(
                new ExampleCustomClaims("example custom claim value")
        );
        acceptConsentRequest.setSession(acceptOAuth2ConsentRequestSession);

        try {
            return oAuth2Api.acceptOAuth2ConsentRequest(consentChallenge, acceptConsentRequest);
        } catch (ApiException e) {
            switch (e.getCode()) {
                case 404, 500 -> throw new RuntimeException("code: " + e.getCode(), e); // jsonError
                default -> throw new RuntimeException("unhandled code: " + e.getCode(), e);
            }
        }
    }

}
