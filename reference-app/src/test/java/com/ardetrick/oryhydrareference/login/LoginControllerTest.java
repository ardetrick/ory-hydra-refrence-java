package com.ardetrick.oryhydrareference.login;

import com.ardetrick.oryhydrareference.login.LoginResult.LoginAcceptedFollowRedirect;
import com.ardetrick.oryhydrareference.login.LoginResult.LoginNotSkippableDisplayLoginUI;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginController.class)
class LoginControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LoginService loginService;

    @Test
    public void loginOptimistically_whenMissingLoginChallengeQueryParam_shouldReturn400() throws Exception {
        val loginChallenge = "example-login-challenge";

        when(loginService.processInitialLoginRequest(loginChallenge))
                .thenReturn(new LoginAcceptedFollowRedirect("redirect-location"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void loginOptimistically_shouldHandleRedirectResponse() throws Exception {
        val loginChallenge = "example-login-challenge";

        when(loginService.processInitialLoginRequest(loginChallenge))
                .thenReturn(new LoginAcceptedFollowRedirect("redirect-location"));

        mockMvc.perform(get("/login").queryParam("login_challenge", loginChallenge))
                .andExpect(status().isFound())
                .andExpect(header().exists("location"))
                .andExpect(header().stringValues("location", "redirect-location"));
    }

    @Test
    public void loginOptimistically_loginDisplay() throws Exception {
        val loginChallenge = "example-login-challenge";

        when(loginService.processInitialLoginRequest(loginChallenge))
                .thenReturn(new LoginNotSkippableDisplayLoginUI(loginChallenge));

        mockMvc.perform(get("/login").queryParam("login_challenge", loginChallenge))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("""
                        <form action="/login/usernamePassword" method="post">
                        """)))
                .andExpect(content().string(containsString("""
                        <input type="hidden" name="loginChallenge" value="example-login-challenge" />
                        """
                )));
    }

}
