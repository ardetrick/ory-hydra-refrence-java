package com.ardetrick.oryhydrareference.hydra;

import lombok.NonNull;
import sh.ory.hydra.model.AcceptOAuth2ConsentRequest;
import sh.ory.hydra.model.AcceptOAuth2ConsentRequestSession;
import sh.ory.hydra.model.AcceptOAuth2LoginRequest;
import sh.ory.hydra.model.RejectOAuth2Request;

public class OryHydraRequestMapper {

  private static final Long DEFAULT_SESSION_EXPIRATION_IN_SECONDS = 3600L;

  public static AcceptOAuth2LoginRequest map(@NonNull final AcceptLoginRequest acceptLoginRequest) {
    // Subject is an alias for user ID. A subject can be a random string, a UUID, an email
    // address, ...
    return new AcceptOAuth2LoginRequest()
        .subject(acceptLoginRequest.subject())
        .remember(acceptLoginRequest.remember())
        .rememberFor(DEFAULT_SESSION_EXPIRATION_IN_SECONDS);
  }

  public static AcceptOAuth2ConsentRequest map(
      @NonNull final AcceptConsentRequest acceptConsentRequest) {
    return new AcceptOAuth2ConsentRequest()
        .grantScope(acceptConsentRequest.scopes())
        .grantAccessTokenAudience(acceptConsentRequest.grantAccessTokenAudience())
        .remember(acceptConsentRequest.remember())
        .rememberFor(DEFAULT_SESSION_EXPIRATION_IN_SECONDS)
        .session(getAcceptOAuth2ConsentRequestSession());
  }

  public static RejectOAuth2Request map(@NonNull final RejectConsentRequest rejectConsentRequest) {
    return new RejectOAuth2Request()
        .error(rejectConsentRequest.error())
        .errorDescription(rejectConsentRequest.errorDescription());
  }

  private static AcceptOAuth2ConsentRequestSession getAcceptOAuth2ConsentRequestSession() {
    // An example of setting custom claims.
    record ExampleCustomClaims(String exampleCustomClaimKey) {}

    return new AcceptOAuth2ConsentRequestSession()
        .idToken(new ExampleCustomClaims("example custom claim value"));
  }
}
