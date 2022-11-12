package com.ardetrick.oryhydrareference.login;

import com.ardetrick.oryhydrareference.modelandview.ModelAndViewUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.val;
import org.springframework.web.servlet.ModelAndView;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class InitialLoginResult {

    String redirectUrl;

    String loginChallenge;

    public static InitialLoginResult loginAcceptedFollowRedirect(String redirectUrl) {
        return InitialLoginResult.builder()
                .redirectUrl(redirectUrl)
                .build();
    }

    public static InitialLoginResult displayLoginUserInterface(String loginChallenge) {
        return InitialLoginResult.builder()
                .loginChallenge(loginChallenge)
                .build();
    }

    public ModelAndView toModelAndView() {
        if (redirectUrl != null) {
            return ModelAndViewUtils.redirect(redirectUrl);
        }

        val loginModelAndView = new ModelAndView();
        loginModelAndView.setViewName("/login");
        loginModelAndView.addObject("loginChallenge", loginChallenge);

        return loginModelAndView;
    }

}
