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

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HydraAdminClient {

    @NonNull OAuth2Api oAuth2Api;

    HydraAdminClient() {
        val defaultClient = Configuration.getDefaultApiClient()
                        .setBasePath("http://localhost");
        oAuth2Api = new OAuth2Api(defaultClient);
    }

    /**
     * https://github.com/ory/hydra-client-java/blob/master/docs/OAuth2Api.md#getoauth2loginrequest
     */
    public Optional<OAuth2LoginRequest> getLoginRequest(@NonNull String loginChallenge) {
        try {
            return Optional.of(oAuth2Api.getOAuth2LoginRequest(loginChallenge));
        } catch (ApiException e) {
            if (400 == e.getCode()) {
                return Optional.empty();
                // jsonError
            }
            if (404 == e.getCode()) {
                return Optional.empty();
                // jsonError
            }
            if (410 == e.getCode()) {
                return Optional.empty();
                // requestWasHandledResponse
            }
            if (500 == e.getCode()) {
                return Optional.empty();
                // jsonError
            }
            throw new RuntimeException(e);
        }
    }

    public OAuth2RedirectTo acceptLoginRequest(@NonNull String loginChallenge) {
        val acceptLoginRequest = new AcceptOAuth2LoginRequest();
        acceptLoginRequest.subject("dummy subject");
        try {
            return oAuth2Api.acceptOAuth2LoginRequest(loginChallenge, acceptLoginRequest);
        } catch (ApiException e) {
            if (400 == e.getCode()) {
                // jsonError
            }
            if (401 == e.getCode()) {
                // jsonError
            }
            if (404 == e.getCode()) {
                // jsonError
            }
            if (500 == e.getCode()) {
                // jsonError
            }
            throw new RuntimeException(e);
        }
    }

    public OAuth2ConsentRequest getConsentRequest(@NonNull String consentChallenge) {
        try {
            return oAuth2Api.getOAuth2ConsentRequest(consentChallenge);
        } catch (ApiException e) {
            if (400 == e.getCode()) {
                // jsonError
            }
            if (404 == e.getCode()) {
                // jsonError
            }
            if (410 == e.getCode()) {
                // requestWasHandledResponse
            }
            if (500 == e.getCode()) {
                // jsonError
            }
            throw new RuntimeException(e);
        }
    }

    public OAuth2RedirectTo acceptConsentRequest(@NonNull String consentChallenge) {
        val acceptConsentRequest = new AcceptOAuth2ConsentRequest();
        try {
            return oAuth2Api.acceptOAuth2ConsentRequest(consentChallenge, acceptConsentRequest);
        } catch (ApiException e) {
            if (404 == e.getCode()) {
                // jsonError
            }
            if (500 == e.getCode()) {
                // jsonError
            }
            throw new RuntimeException(e);
        }
    }

}
