package com.ardetrick.oryhydrareference.hydra;

import lombok.NonNull;
import sh.ory.hydra.model.AcceptOAuth2ConsentRequest;
import sh.ory.hydra.model.AcceptOAuth2ConsentRequestSession;

public class OryHydraRequestMapper {

    private static final Long DEFAULT_SESSION_EXPIRATION_IN_SECONDS = 3600L;

    public static AcceptOAuth2ConsentRequest map(@NonNull final AcceptConsentRequest acceptConsentRequest) {
        return new AcceptOAuth2ConsentRequest()
                .grantScope(acceptConsentRequest.scopes())
                .grantAccessTokenAudience(acceptConsentRequest.grantAccessTokenAudience())
                .remember(acceptConsentRequest.remember())
                .rememberFor(DEFAULT_SESSION_EXPIRATION_IN_SECONDS)
                .session(getAcceptOAuth2ConsentRequestSession());
    }

    private static AcceptOAuth2ConsentRequestSession getAcceptOAuth2ConsentRequestSession() {
        // An example of setting custom claims.
        record ExampleCustomClaims(String exampleCustomClaimKey) {}

        return new AcceptOAuth2ConsentRequestSession()
                .idToken(new ExampleCustomClaims("example custom claim value"));
    }

}
