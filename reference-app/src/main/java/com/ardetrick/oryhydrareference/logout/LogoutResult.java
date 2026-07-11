package com.ardetrick.oryhydrareference.logout;

public sealed interface LogoutResult
    permits LogoutResult.DisplayLogoutUI,
        LogoutResult.LogoutAcceptedFollowRedirect,
        LogoutResult.LogoutCancelledReturnHome,
        LogoutResult.LogoutRequestNotFound {

  record DisplayLogoutUI(String logoutChallenge, String subject) implements LogoutResult {}

  record LogoutAcceptedFollowRedirect(String redirectUrl) implements LogoutResult {}

  record LogoutCancelledReturnHome() implements LogoutResult {}

  record LogoutRequestNotFound() implements LogoutResult {}
}
