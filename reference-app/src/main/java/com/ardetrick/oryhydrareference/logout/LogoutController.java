package com.ardetrick.oryhydrareference.logout;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/logout")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutController {

  @NonNull LogoutService logoutService;

  /**
   * The Logout Endpoint (set by urls.logout) is an application written by you. Ory Hydra redirects
   * here with a logout_challenge when a logout is requested (for example via {@code
   * /oauth2/sessions/logout}). The endpoint fetches the logout request from the admin API and asks
   * the user to confirm, then accepts or rejects the challenge.
   *
   * @see <a href="https://www.ory.sh/docs/hydra/guides/logout">Implementing The Logout Endpoint</a>
   */
  @GetMapping
  public ModelAndView logoutEndpoint(
      @RequestParam("logout_challenge") final String logoutChallenge) {
    val response = logoutService.processInitialLogoutRequest(logoutChallenge);

    return LogoutModelAndViewMapper.map(response);
  }

  @PostMapping
  public ModelAndView submitLogoutForm(final LogoutForm logoutForm) {
    val response = logoutService.processLogoutForm(logoutForm);

    return LogoutModelAndViewMapper.map(response);
  }
}
