package com.ardetrick.oryhydrareference.login;

sealed interface LoginResult permits LoginAcceptedFollowRedirect, LoginDeniedInvalidCredentials {}

record LoginAcceptedFollowRedirect(String redirectUrl) implements LoginResult {}

record LoginDeniedInvalidCredentials() implements LoginResult {}

//record LoginResult(String redirectUrl) {
//
//    public static LoginResult loginAcceptedFollowRedirect(String redirectUrl) {
//        return new LoginResult(redirectUrl);
//    }
//
//    public static LoginResult invalidCredentials() {
//        return new LoginResult(null);
//    }
//
//}
