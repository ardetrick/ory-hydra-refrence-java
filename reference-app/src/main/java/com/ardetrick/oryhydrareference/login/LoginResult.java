package com.ardetrick.oryhydrareference.login;

import org.springframework.web.servlet.ModelAndView;

record LoginResult(String redirectUrl) {

    public static LoginResult loginAcceptedFollowRedirect(String redirectUrl) {
        return new LoginResult(redirectUrl);
    }

    public static LoginResult invalidCredentials() {
        return new LoginResult(null);
    }

}
