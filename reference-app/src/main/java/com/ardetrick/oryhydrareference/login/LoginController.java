package com.ardetrick.oryhydrareference.login;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/login")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginController {

    @NonNull LoginService loginService;

    /**
     * This is the endpoint which Ory Hydra redirects to after a resource owner initiates the OAuth flow. It is
     * responsible for verifying the login challenge. Based on the current authentication state returned from Hydra
     * the OAuth flow may be continued (the "optimistic" login case) or they will be redirected to a UI, so they can
     * explicitly authenticate themselves.
     */
    @GetMapping
    public ModelAndView loginOptimistically(@RequestParam("login_challenge") String loginChallenge) {
        val loginResult = loginService.processInitialLoginRequest(loginChallenge);

        return LoginModelAndViewMapper.toView(loginResult, loginChallenge);
    }

    /**
     * An implementation of the Ory Hydra Login Endpoint. This endpoint is responsible for fulfilling the
     * responsibilities of the login endpoint as outlined by the Ory Hydra documentation.
     * </p>
     * One of the requirements of this endpoint is to authenticate the user. As per the OAuth specification
     * the identity of the resource owner must be verified, but the way in which that authentication is done is not
     * specified.
     * </p>
     * For the purposes of this demo a credentials are used but other approaches such as session cookies could be used.
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc6749#section-3.1">RFC 6749 Section 3.1</a>
     * @see <a href="https://www.ory.sh/docs/hydra/guides/login">Implementing The Login Endpoint</a>
     */
    @PostMapping("usernamePassword")
    public ModelAndView loginWithUsernamePasswordForm(
            LoginForm loginForm
    ) {
        val loginResult = loginService.processSubmittedLoginRequest(loginForm.loginChallenge(), loginForm);

        return LoginModelAndViewMapper.toView(loginResult, loginForm.loginChallenge());
    }

}
