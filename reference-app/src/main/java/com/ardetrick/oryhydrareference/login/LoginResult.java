package com.ardetrick.oryhydrareference.login;

sealed interface LoginResult permits
        LoginNotSkippableDisplayLoginUI,
        LoginAcceptedFollowRedirect,
        LoginDeniedInvalidCredentials,
        LoginRequestNotFound {}

record LoginAcceptedFollowRedirect(String redirectUrl) implements LoginResult {}

record LoginDeniedInvalidCredentials() implements LoginResult {}

record LoginRequestNotFound() implements LoginResult {}

record LoginNotSkippableDisplayLoginUI(String loginChallenge) implements LoginResult {}
