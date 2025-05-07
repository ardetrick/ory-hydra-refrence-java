package com.ardetrick.oryhydrareference.login;

public sealed interface LoginResult permits
        LoginResult.LoginNotSkippableDisplayLoginUI,
        LoginResult.LoginAcceptedFollowRedirect,
        LoginResult.LoginDeniedInvalidCredentials,
        LoginResult.LoginRequestNotFound {

    record LoginAcceptedFollowRedirect(String redirectUrl) implements LoginResult {}

    record LoginDeniedInvalidCredentials() implements LoginResult {}

    record LoginRequestNotFound() implements LoginResult {}

    record LoginNotSkippableDisplayLoginUI(String loginChallenge) implements LoginResult {}


}
