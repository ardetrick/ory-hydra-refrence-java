package com.ardetrick.oryhydrareference.demo;

import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The landing page: lists registered OAuth2 clients with ready-to-click links that start a real
 * authorization code flow. Demonstration purposes only — a production Hydra deployment has no page
 * like this.
 */
@RequiredArgsConstructor
@Controller
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DemoController {

  @NonNull HydraAdminClient hydraAdminClient;
  @NonNull HydraAdminClient.Properties properties;

  @GetMapping("/")
  public ModelAndView landingPage() {
    // The app's own demo callback page; flows that redirect here complete in the browser.
    val appCallbackUri =
        ServletUriComponentsBuilder.fromCurrentContextPath().path("/callback").toUriString();

    val clients =
        hydraAdminClient.listOAuth2Clients().stream()
            .map(
                client ->
                    new MvcClient(
                        client.getClientName(),
                        client.getClientId(),
                        client.getRedirectUris().stream()
                            .map(
                                redirectUri ->
                                    new MvcClient.StartLink(
                                        redirectUri,
                                        authorizeUrl(
                                            client.getClientId(), client.getScope(), redirectUri),
                                        redirectUri.equals(appCallbackUri)))
                            .toList()))
            .toList();

    return new ModelAndView("demo").addObject("clients", clients);
  }

  /** The landing page used to live here; redirect so old links and muscle memory keep working. */
  @GetMapping("/demo")
  public RedirectView demo() {
    return new RedirectView("/", true);
  }

  private String authorizeUrl(String clientId, String scope, String redirectUri) {
    return UriComponentsBuilder.fromUriString(properties.getPublicBasePath())
        .path("/oauth2/auth")
        .queryParam("client_id", clientId)
        .queryParam("response_type", "code")
        .queryParam("redirect_uri", redirectUri)
        .queryParam("scope", scope == null || scope.isBlank() ? "openid offline" : scope)
        .queryParam("state", UUID.randomUUID().toString())
        .encode()
        .build()
        .toUriString();
  }
}
