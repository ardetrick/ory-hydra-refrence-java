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

        // TODO:
        // look at request body to see if they accepted the request
        // - if they denied, call the admin endpoint to reject the request

        // Naive authentication logic. In reality this should delegate to a real authentication system.
        if (loginForm.getLoginEmail() == null || loginForm.getLoginPassword() == null) {
            return LoginResult.invalidCredentials();
        }
        if (!"foo@bar.com".equals(loginForm.getLoginEmail()) || !"password".equals(loginForm.getLoginPassword())) {
            return LoginResult.invalidCredentials();
        }

        val completedRequest = hydraAdminClient.acceptLoginRequest(loginChallenge, loginForm.getLoginEmail());

        return LoginResult.loginAcceptedFollowRedirect(completedRequest.getRedirectTo());
    }

}
