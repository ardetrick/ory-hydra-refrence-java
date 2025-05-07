package com.ardetrick.oryhydrareference.login;

import com.ardetrick.oryhydrareference.login.LoginResult.LoginAcceptedFollowRedirect;
import com.ardetrick.oryhydrareference.login.LoginResult.LoginNotSkippableDisplayLoginUI;
import com.ardetrick.oryhydrareference.login.LoginResult.LoginRequestNotFound;
import com.ardetrick.oryhydrareference.modelandview.ModelAndViewUtils;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.springframework.web.servlet.ModelAndView;

@UtilityClass
public class LoginModelAndViewMapper {

    public static ModelAndView toView(@NonNull final LoginResult loginResult,
                                      @NonNull final String loginChallenge) {
        return switch (loginResult) {
            case LoginAcceptedFollowRedirect(String redirectUrl) ->
                    ModelAndViewUtils.redirectToDifferentContext(redirectUrl);
            case LoginResult.LoginDeniedInvalidCredentials ignored -> {
                val loginModelAndView = new ModelAndView();
                loginModelAndView.setViewName("/login");
                loginModelAndView.addObject("loginChallenge", loginChallenge);
                loginModelAndView.addObject("error", "invalid credentials try again");

                yield loginModelAndView;
            }
            case LoginNotSkippableDisplayLoginUI ignored -> {
                val loginModelAndView = new ModelAndView();
                loginModelAndView.setViewName("/login");
                loginModelAndView.addObject("loginChallenge", loginChallenge);

                yield loginModelAndView;
            }
            case LoginRequestNotFound ignored -> new ModelAndView("/home");
        };
    }

}
