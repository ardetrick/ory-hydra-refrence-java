package com.ardetrick.oryhydrareference.login;

import com.ardetrick.oryhydrareference.modelandview.ModelAndViewUtils;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.springframework.web.servlet.ModelAndView;

@UtilityClass
public class LoginModelAndViewMapper {

    public static ModelAndView toView(@NonNull final LoginResult loginResult,
                                      @NonNull final LoginForm loginForm) {
        if (loginResult instanceof LoginAcceptedFollowRedirect acceptedFollowRedirect) {
            return ModelAndViewUtils.redirectToDifferentContext(acceptedFollowRedirect.redirectUrl());
        }

        if (loginResult instanceof LoginDeniedInvalidCredentials) {
            val loginModelAndView = new ModelAndView();
            loginModelAndView.setViewName("/login");
            loginModelAndView.addObject("loginChallenge", loginForm.loginChallenge());
            loginModelAndView.addObject("error", "invalid credentials try again");

            return loginModelAndView;
        }

        throw new IllegalStateException("Unknown response type: " + loginResult.getClass());
    }

}
