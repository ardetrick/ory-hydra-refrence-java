package com.ardetrick.oryhydrareference.demo;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * A demo-only OAuth2 client callback. In a real deployment the callback belongs to the client
 * application, not the login/consent provider — this page exists so the demo flow can complete in
 * the browser: it shows the authorization code (or error) Hydra redirected back with and exchanges
 * the code for tokens at the click of a button.
 */
@RequiredArgsConstructor
@Controller
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DemoCallbackController {

  @NonNull DemoTokenExchangeService demoTokenExchangeService;

  @GetMapping("/callback")
  public ModelAndView callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription) {
    // The token exchange must present the exact redirect_uri used in the authorization request,
    // which for this page is its own URL.
    val redirectUri =
        ServletUriComponentsBuilder.fromCurrentContextPath().path("/callback").toUriString();

    return new ModelAndView("callback")
        .addObject("code", code)
        .addObject("error", error)
        .addObject("errorDescription", errorDescription)
        .addObject("redirectUri", redirectUri);
  }

  @PostMapping("/callback/exchange")
  public ModelAndView exchange(final ExchangeForm exchangeForm) {
    val result = demoTokenExchangeService.exchange(exchangeForm);

    return new ModelAndView("tokens")
        .addObject("tokenResponse", result.prettyTokenResponse())
        .addObject("idTokenClaims", result.prettyIdTokenClaims());
  }
}
