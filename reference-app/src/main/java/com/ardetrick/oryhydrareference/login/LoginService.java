package com.ardetrick.oryhydrareference.login;

import com.ardetrick.oryhydrareference.hydra.AcceptLoginRequest;
import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import com.ardetrick.oryhydrareference.login.LoginResult.LoginAcceptedFollowRedirect;
import com.ardetrick.oryhydrareference.login.LoginResult.LoginDeniedInvalidCredentials;
import com.ardetrick.oryhydrareference.login.LoginResult.LoginNotSkippableDisplayLoginUI;
import com.ardetrick.oryhydrareference.login.LoginResult.LoginRequestNotFound;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginService {

  @NonNull HydraAdminClient hydraAdminClient;

  public LoginResult processInitialLoginRequest(@NonNull String loginChallenge) {
    val maybeLoginRequest = hydraAdminClient.getLoginRequest(loginChallenge);
    if (maybeLoginRequest.isEmpty()) {
      return new LoginRequestNotFound();
    }

    val loginRequest = maybeLoginRequest.get();

    if (loginRequest.getSkip()) {
      // The session is already remembered, so there is nothing new to remember. Follow the
      // accept response's redirect (redirecting back to the original request URL would replay
      // the authorization request and loop straight back here).
      val completedRequest =
          hydraAdminClient.acceptLoginRequest(
              AcceptLoginRequest.builder()
                  .loginChallenge(loginChallenge)
                  .subject(loginRequest.getSubject())
                  .remember(false)
                  .build());
      return new LoginAcceptedFollowRedirect(completedRequest.getRedirectTo());
    }

    return new LoginNotSkippableDisplayLoginUI(loginChallenge);
  }

  public LoginResult processSubmittedLoginRequest(
      @NonNull String loginChallenge, LoginForm loginForm) {
    val maybeLoginRequest = hydraAdminClient.getLoginRequest(loginChallenge);
    if (maybeLoginRequest.isEmpty()) {
      return new LoginRequestNotFound();
    }

    val maybeInvalidCredentialsResult = checkForInvalidCredentialsResult(loginForm);
    if (maybeInvalidCredentialsResult.isPresent()) {
      return maybeInvalidCredentialsResult.get();
    }

    val completedRequest =
        hydraAdminClient.acceptLoginRequest(
            AcceptLoginRequest.builder()
                .loginChallenge(loginChallenge)
                .subject(loginForm.loginEmail())
                .remember(loginForm.isRemember())
                .build());

    return new LoginAcceptedFollowRedirect(completedRequest.getRedirectTo());
  }

  private static Optional<LoginDeniedInvalidCredentials> checkForInvalidCredentialsResult(
      LoginForm loginForm) {
    // Naive authentication logic. In reality this should delegate to a real authentication system.
    if (loginForm.loginEmail() == null || loginForm.loginPassword() == null) {
      return Optional.of(new LoginDeniedInvalidCredentials());
    }
    if (!"foo@bar.com".equals(loginForm.loginEmail())
        || !"password".equals(loginForm.loginPassword())) {
      return Optional.of(new LoginDeniedInvalidCredentials());
    }

    return Optional.empty();
  }
}
