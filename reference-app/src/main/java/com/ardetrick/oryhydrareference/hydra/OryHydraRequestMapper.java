package com.ardetrick.oryhydrareference.hydra;

import lombok.NonNull;
import lombok.val;
import sh.ory.hydra.model.AcceptOAuth2ConsentRequest;
import sh.ory.hydra.model.AcceptOAuth2ConsentRequestSession;

public class OryHydraRequestMapper {

    private static final Long DEFAULT_SESSION_EXPIRATION_IN_SECONDS = 3600L;

    public static AcceptOAuth2ConsentRequest map(@NonNull final AcceptConsentRequest acceptConsentRequest) {
        val acceptOAuth2ConsentRequest = new AcceptOAuth2ConsentRequest();
        acceptOAuth2ConsentRequest.setGrantScope(acceptConsentRequest.scopes());
        acceptOAuth2ConsentRequest.setGrantAccessTokenAudience(acceptConsentRequest.grantAccessTokenAudience());
        acceptOAuth2ConsentRequest.remember(acceptConsentRequest.remember());
        acceptOAuth2ConsentRequest.rememberFor(DEFAULT_SESSION_EXPIRATION_IN_SECONDS);

        val acceptOAuth2ConsentRequestSession = new AcceptOAuth2ConsentRequestSession();

        // An example of setting custom claims.
        record ExampleCustomClaims(String exampleCustomClaimKey) {}
        acceptOAuth2ConsentRequestSession.setIdToken(
                new ExampleCustomClaims("example custom claim value")
        );
        acceptOAuth2ConsentRequest.setSession(acceptOAuth2ConsentRequestSession);

        return acceptOAuth2ConsentRequest;
    }

}
