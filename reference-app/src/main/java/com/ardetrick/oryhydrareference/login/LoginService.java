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
            hydraAdminClient.acceptLoginRequest(loginChallenge);
            return InitialLoginResult.loginAcceptedFollowRedirect(loginRequest.getRequestUrl());
        }

        return InitialLoginResult.displayLoginUserInterface(loginChallenge);
    }

    public String processSubmittedLoginRequest(@NonNull String loginChallenge, LoginForm loginForm) {
        hydraAdminClient.getLoginRequest(loginChallenge)
                .orElseThrow(LoginRequestNotFoundException::new);

        // TODO:
        // look at request body to see if they accepted the request
        // - if they denied, call the admin endpoint to reject the request

        // validate the request

        // validate the request
        // - this could be done by doing password credentials
        // - or looking at cookies
        // - or many other methods

        val completedRequest = hydraAdminClient.acceptLoginRequest(loginChallenge);

        return completedRequest.getRedirectTo();
    }

}
