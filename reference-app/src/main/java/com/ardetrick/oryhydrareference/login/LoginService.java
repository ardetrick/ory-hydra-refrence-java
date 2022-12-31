package com.ardetrick.oryhydrareference.login;

import com.ardetrick.oryhydrareference.exception.LoginRequestNotFoundException;
import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginService {

    @NonNull HydraAdminClient hydraAdminClient;

    public InitialLoginResult processInitialLoginRequest(@NonNull String loginChallenge) {
        val loginRequest = hydraAdminClient.getLoginRequest(loginChallenge)
                .orElseThrow(LoginRequestNotFoundException::new);

        if (loginRequest.getSkip()) {
            hydraAdminClient.acceptLoginRequest(loginChallenge, loginRequest.getSubject());
            return InitialLoginResult.loginAcceptedFollowRedirect(loginRequest.getRequestUrl());
        }

        return InitialLoginResult.displayLoginUserInterface(loginChallenge);
    }

    public LoginResult processSubmittedLoginRequest(@NonNull String loginChallenge, LoginForm loginForm) {
        hydraAdminClient.getLoginRequest(loginChallenge)
                .orElseThrow(LoginRequestNotFoundException::new);

        val maybeInvalidCredentialsResult = checkForInvalidCredentaialsResult(loginForm);
        if (maybeInvalidCredentialsResult.isPresent()) {
            return maybeInvalidCredentialsResult.get();
        }

        val completedRequest = hydraAdminClient.acceptLoginRequest(loginChallenge, loginForm.loginEmail());

        return new LoginAcceptedFollowRedirect(completedRequest.getRedirectTo());
    }

    private static Optional<LoginDeniedInvalidCredentials> checkForInvalidCredentaialsResult(LoginForm loginForm) {
        // Naive authentication logic. In reality this should delegate to a real authentication system.
        if (loginForm.loginEmail() == null || loginForm.loginPassword() == null) {
            return Optional.of(new LoginDeniedInvalidCredentials());
        }
        if (!"foo@bar.com".equals(loginForm.loginEmail()) || !"password".equals(loginForm.loginPassword())) {
            return Optional.of(new LoginDeniedInvalidCredentials());
        }

        return Optional.empty();
    }

}
