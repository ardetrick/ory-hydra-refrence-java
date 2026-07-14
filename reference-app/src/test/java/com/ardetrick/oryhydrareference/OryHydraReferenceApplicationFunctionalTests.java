package com.ardetrick.oryhydrareference;

import static com.ardetrick.oryhydrareference.ClientCallBackController.CLIENT_CALL_BACK_PATH;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import com.ardetrick.oryhydrareference.test.utils.ScreenshotPathProducer;
import com.ardetrick.testcontainers.OryHydraContainer;
import com.auth0.jwt.JWT;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URIBuilder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
  ClientCallBackController.class,
  ForwardingController.class,
})
@TestPropertySource(properties = {"debug=true"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OryHydraReferenceApplicationFunctionalTests {

  // Shared between all tests in this class — plain instance fields: under
  // @TestInstance(PER_CLASS) the single test instance lives for the whole class.
  private Playwright playwright;
  private Browser browser;

  @LocalServerPort int springBootAppPort;

  OryHydraContainer oryHydraContainer;

  String clientId;
  String clientSecret;
  String redirectUri;

  @Autowired HydraAdminClient.Properties properties;

  @MockitoBean Consumer<String> queryStringConsumer;

  @Captor
  ArgumentCaptor<String> queryStringConsumerArgumentCaptor = ArgumentCaptor.forClass(String.class);

  @Autowired ForwardingController forwardingController;

  // Lifecycle: the Spring context (and its random-port Tomcat) boots before JUnit creates the
  // test instance; @TestInstance(PER_CLASS) reuses that single instance for the whole class,
  // which is what allows @BeforeAll to be non-static — and therefore able to read the injected
  // @LocalServerPort — while still running exactly once before all tests.
  //
  // Deliberately NOT the idiomatic @Testcontainers/@Container-on-a-static-field pattern: that
  // constructs the container at class-load time, but this container's configuration needs the
  // injected @LocalServerPort, which only exists after the context boots. The alternatives all
  // cost more than they save (a DEFINED_PORT app breaks parallel safety; an instance @Container
  // restarts per test regardless of @TestInstance).
  @BeforeAll
  void startTestEnvironment() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch();

    // One Hydra container serves every test in this class — containers are the expensive
    // resource. Test isolation is preserved by registering a unique OAuth2 client per test
    // instead (see registerTestClient).
    oryHydraContainer =
        OryHydraContainer.builder()
            .urlsLogin("http://localhost:" + springBootAppPort + "/login")
            .urlsConsent("http://localhost:" + springBootAppPort + "/consent")
            .urlsLogout("http://localhost:" + springBootAppPort + "/logout")
            .urlsSelfIssuer(
                "http://localhost:" + springBootAppPort + "/integration-test-public-proxy")
            .build();
    oryHydraContainer.start();

    // Late-bind the two Hydra endpoints now that the mapped ports exist (the circular
    // port dependency: the app and Hydra each need the other's randomized URI). Two consumers,
    // two different endpoints: the app's HydraAdminClient speaks the admin API, while the
    // test's public proxy forwards browsers to the public authorize endpoint.
    properties.setBasePath(oryHydraContainer.adminBaseUriString());
    // The landing page and demo callback build public-endpoint URLs from this property.
    properties.setPublicBasePath(oryHydraContainer.publicBaseUriString());
    forwardingController.hydraPublicBaseUri = oryHydraContainer.publicBaseUriString();
  }

  @AfterAll
  void stopTestEnvironment() {
    playwright.close();
    oryHydraContainer.stop();
  }

  @BeforeEach
  public void registerTestClient() {
    // A unique client per test keeps tests isolated on the shared container: Hydra remembers
    // consent per subject + client, so a shared client would leak remembered consent between
    // tests. Registration is a cheap upserting admin API call via the container.
    clientId = "test-client-" + UUID.randomUUID();
    clientSecret = "client-secret";
    redirectUri = "http://localhost:" + springBootAppPort + CLIENT_CALL_BACK_PATH;
    oryHydraContainer.createOrReplaceClient(
        client ->
            client
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUris(redirectUri)
                .grantTypes("authorization_code", "refresh_token")
                .responseTypes("code", "id_token")
                .scope("offline_access", "openid", "offline", "profile"));
  }

  /**
   * Tests that the .well-known/jwks.json URI is exposed and accessible.
   *
   * @link <a
   *     href="https://www.ory.sh/docs/reference/api#tag/wellknown/operation/discoverJsonWebKeys"/>
   */
  @Test
  void requestToJwksUriReturns200() throws IOException, InterruptedException {
    val request =
        HttpRequest.newBuilder(
                URI.create(oryHydraContainer.publicBaseUriString() + "/.well-known/jwks.json"))
            .build();
    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  public void loginInvalidCredentials() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder().testName("loginInvalidCredentials").build();

    val page = browser.newPage();
    val initiateFlowUri = getUriToInitiateFlow();

    page.navigate(initiateFlowUri.toString());
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password1");

    page.locator("input[name=submit]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

    assertThat(page.content()).contains("invalid credentials try again");
  }

  private URI getUriToInitiateFlow() {
    try {
      return new URIBuilder(oryHydraContainer.publicBaseUriString() + "/oauth2/auth")
          .addParameter("response_type", "code")
          .addParameter("client_id", clientId)
          .addParameter("redirect_uri", redirectUri)
          .addParameter("scope", "offline_access openid offline profile")
          .addParameter("state", "12345678901234567890")
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void completeFullOAuthFlowUsingUIToLogin() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder().testName("completeFullOAuthFlowUsingUIToLogin").build();

    val page = browser.newPage();

    val uri = getUriToInitiateFlow();

    page.navigate(uri.toString());
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");

    page.locator("input[name=submit]").click();

    page.waitForLoadState();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

    page.locator("input[id=accept]").click();

    page.waitForLoadState();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

    val code = getCodeFromCallbackCaptor();
    val token = exchangeCode(code);

    assertThat(token.accessToken()).isNotBlank();
    assertThat(token.refreshToken()).isNotBlank();
    assertThat(token.idToken()).isNotBlank();

    val decodedJWT = JWT.decode(token.idToken());
    assertThat(decodedJWT.getClaim("exampleCustomClaimKey").asString())
        .isNotNull()
        .isEqualTo("example custom claim value");
  }

  @Test
  public void completeFlowWithPartialScopeSelection() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder().testName("completeFlowWithPartialScopeSelection").build();

    val page = browser.newPage();

    val uri = getUriToInitiateFlow();

    page.navigate(uri.toString());
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");

    page.locator("input[name=submit]").click();

    page.waitForLoadState();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

    page.locator("input[id=scopes-profile]").uncheck();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-uncheck-scope"));

    page.locator("input[id=accept]").click();

    page.waitForLoadState();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

    val code = getCodeFromCallbackCaptor();
    val token = exchangeCode(code);

    assertThat(token.accessToken()).isNotBlank();
    assertThat(token.refreshToken()).isNotBlank();
    assertThat(token.idToken()).isNotBlank();
    assertThat(token.scope()).doesNotContain("profile");
  }

  private CodeExchangeResponse exchangeCode(String code) {
    val encodedParams =
        Map.of(
                "client_id", clientId,
                "code", code,
                "grant_type", "authorization_code",
                "redirect_uri", redirectUri)
            .entrySet()
            .stream()
            .map(
                entry ->
                    String.join(
                        "=",
                        URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8),
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)))
            .collect(Collectors.joining("&"));

    val request =
        HttpRequest.newBuilder()
            .uri(URI.create(oryHydraContainer.publicBaseUriString() + "/oauth2/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header(
                "authorization",
                "Basic "
                    + Base64.getEncoder()
                        .encodeToString((clientId + ":" + clientSecret).getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(encodedParams))
            .build();

    HttpResponse<String> codeExchangeResponse;
    try {
      codeExchangeResponse =
          HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    assertThat(codeExchangeResponse.statusCode()).isEqualTo(200);

    try {
      return new ObjectMapper().readValue(codeExchangeResponse.body(), CodeExchangeResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void skipConsentScreenOnSecondLoginWhenRememberMeIsUsed() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder()
            .testName("skipConsentScreenOnSecondLoginWhenRememberMeIsUsed")
            .build();
    val page = browser.newPage();

    val uri = getUriToInitiateFlow();

    page.navigate(uri.toString());
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");

    page.locator("input[name=submit]").click();

    page.waitForLoadState();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

    page.locator("input[id=accept]").click();

    page.waitForLoadState();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

    val code = getCodeFromCallbackCaptor();
    exchangeCode(code);

    page.navigate(getUriToInitiateFlow().toString());
    page.screenshot(
        screenshotPathProducer.screenshotOptionsForStepName("initial-load-second-time"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");

    page.locator("input[name=submit]").click();

    page.waitForLoadState();

    page.screenshot(
        screenshotPathProducer.screenshotOptionsForStepName("after-login-submit-second-time"));

    // Consent screen is skipped
    assertThat(page.url()).contains("/client/callback?code=");
  }

  private String getCallbackQueryString() {
    verify(queryStringConsumer).accept(queryStringConsumerArgumentCaptor.capture());
    return queryStringConsumerArgumentCaptor.getValue();
  }

  private String getCodeFromCallbackCaptor() {
    return Arrays.stream(getCallbackQueryString().split("&"))
        .filter(queryStringParam -> queryStringParam.startsWith("code="))
        .findFirst()
        .map(queryStringParam -> queryStringParam.replace("code=", ""))
        .orElseThrow();
  }

  @Test
  public void doNotSkipConsentScreenOnSecondLoginWhenRememberMeIsFalse() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder()
            .testName("doNotSkipConsentScreenOnSecondLoginWhenRememberMeIsFalse")
            .build();

    val page = browser.newPage();

    val uri = getUriToInitiateFlow();

    page.navigate(uri.toString());
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");

    page.locator("input[name=submit]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

    page.locator("input[id=remember]").uncheck();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-uncheck"));

    page.locator("input[id=accept]").click();

    page.waitForLoadState();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

    val code = getCodeFromCallbackCaptor();
    exchangeCode(code);

    page.navigate(getUriToInitiateFlow().toString());
    page.screenshot(
        screenshotPathProducer.screenshotOptionsForStepName("initial-load-second-time"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");

    page.locator("input[name=submit]").click();

    page.waitForLoadState();

    page.screenshot(
        screenshotPathProducer.screenshotOptionsForStepName("after-login-submit-second-time"));

    // Consent screen is not skipped
    assertThat(page.url()).contains("/consent");
  }

  @Test
  public void denyConsentRedirectsToClientWithAccessDeniedError() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder()
            .testName("denyConsentRedirectsToClientWithAccessDeniedError")
            .build();

    val page = browser.newPage();

    page.navigate(getUriToInitiateFlow().toString());
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");

    page.locator("input[name=submit]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

    page.locator("input[id=reject]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-deny"));

    // The browser lands on the client's callback carrying a real OAuth error instead of a code.
    assertThat(page.url()).contains("/client/callback?error=access_denied");

    val queryString = getCallbackQueryString();
    assertThat(queryString).contains("error=access_denied");
    assertThat(queryString).doesNotContain("code=");
  }

  @Test
  public void skipLoginScreenOnSecondFlowWhenLoginRememberMeIsUsed() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder()
            .testName("skipLoginScreenOnSecondFlowWhenLoginRememberMeIsUsed")
            .build();

    val page = browser.newPage();

    page.navigate(getUriToInitiateFlow().toString());
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");
    page.locator("input[id=remember]").check();

    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-check-remember"));

    page.locator("input[name=submit]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

    // Uncheck the consent-side remember so the second flow isolates the login skip: consent must
    // be asked again, proving the login screen alone was skipped.
    page.locator("input[id=remember]").uncheck();

    page.locator("input[id=accept]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

    val code = getCodeFromCallbackCaptor();
    exchangeCode(code);

    page.navigate(getUriToInitiateFlow().toString());

    page.waitForLoadState();
    page.screenshot(
        screenshotPathProducer.screenshotOptionsForStepName("initial-load-second-time"));

    // The login screen is skipped — no credentials were entered this time — but the consent
    // screen still appears because its remember was unchecked.
    assertThat(page.url()).contains("/consent");
  }

  @Test
  public void logoutEndsTheRememberedLoginSession() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder().testName("logoutEndsTheRememberedLoginSession").build();

    val page = browser.newPage();

    // First flow: log in with remember-me so a durable login session exists.
    completeFullFlowWithLoginRemember(page, screenshotPathProducer);

    // Second flow: no credentials needed — proof the session is live before logging out.
    page.navigate(getUriToInitiateFlow().toString());
    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("second-flow-skips-login"));
    assertThat(page.url()).doesNotContain("/login");

    // RP-initiated logout: Hydra redirects to this app's logout endpoint with a challenge.
    page.navigate(oryHydraContainer.publicBaseUriString() + "/oauth2/sessions/logout");
    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("logout-confirmation"));
    assertThat(page.url()).contains("/logout?logout_challenge=");

    page.locator("input[id=confirm]").click();
    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-logout-confirm"));
    assertThat(page.url()).contains("/oauth2/fallbacks/logout");

    // Third flow: the session is gone, so the login screen is back.
    page.navigate(getUriToInitiateFlow().toString());
    page.waitForLoadState();
    page.screenshot(
        screenshotPathProducer.screenshotOptionsForStepName("flow-after-logout-requires-login"));
    assertThat(page.url()).contains("/login");
  }

  @Test
  public void cancellingLogoutKeepsTheSessionAlive() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder().testName("cancellingLogoutKeepsTheSessionAlive").build();

    val page = browser.newPage();

    completeFullFlowWithLoginRemember(page, screenshotPathProducer);

    page.navigate(oryHydraContainer.publicBaseUriString() + "/oauth2/sessions/logout");
    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("logout-confirmation"));

    page.locator("input[id=cancel]").click();
    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-logout-cancel"));

    // The session survives a cancelled logout: the next flow still skips the login screen.
    page.navigate(getUriToInitiateFlow().toString());
    page.waitForLoadState();
    page.screenshot(
        screenshotPathProducer.screenshotOptionsForStepName("flow-after-cancel-skips-login"));
    assertThat(page.url()).doesNotContain("/login");
  }

  @Test
  public void quickStartFromLandingPageExchangesTokensInBrowser() {
    val screenshotPathProducer =
        ScreenshotPathProducer.builder()
            .testName("quickStartFromLandingPageExchangesTokensInBrowser")
            .build();

    // Re-register this test's client to redirect to the app's own demo callback page, the
    // configuration the landing page's quick start is built around.
    val appCallback = "http://localhost:" + springBootAppPort + "/callback";
    oryHydraContainer.createOrReplaceClient(
        client ->
            client
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUris(appCallback)
                .grantTypes("authorization_code", "refresh_token")
                .responseTypes("code", "id_token")
                .scope("offline_access", "openid", "offline", "profile"));

    val page = browser.newPage();

    page.navigate("http://localhost:" + springBootAppPort + "/");
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("landing-page"));

    page.locator("a[data-testid='start-" + clientId + "']").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("login"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");
    page.locator("input[name=submit]").click();

    page.waitForLoadState();
    page.locator("input[id=accept]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("callback-page"));
    assertThat(page.url()).contains("/callback?code=");

    // The form pre-fills the seeded demo client's values; this test's client is unique, so
    // overwrite them.
    page.locator("input[id=clientId]").fill(clientId);
    page.locator("input[id=clientSecret]").fill(clientSecret);
    page.locator("input[id=exchange]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("tokens"));
    assertThat(page.content()).contains("access_token");
    assertThat(page.content()).contains("exampleCustomClaimKey");
  }

  /**
   * Runs one full authorization-code flow with login remember-me checked, ending at the client
   * callback with the code exchanged.
   */
  private void completeFullFlowWithLoginRemember(
      Page page, ScreenshotPathProducer screenshotPathProducer) {
    page.navigate(getUriToInitiateFlow().toString());
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

    page.locator("input[name=loginEmail]").fill("foo@bar.com");
    page.locator("input[name=loginPassword]").fill("password");
    page.locator("input[id=remember]").check();

    page.locator("input[name=submit]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

    page.locator("input[id=accept]").click();

    page.waitForLoadState();
    page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

    val code = getCodeFromCallbackCaptor();
    exchangeCode(code);
  }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record CodeExchangeResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("id_token") String idToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("scope") String scope,
    @JsonProperty("token_type") String tokenType) {}

/**
 * A controller that exposes an endpoint which acts as a proxy to the oauth server. This proxy is
 * used to break the unfortunate circular dependency between Hydra and the reference app. They each
 * need to know about each other at startup, but within the context of testing both are using random
 * ports, so it is impossible for each to know about each other until after the ports have been
 * assigned. By using this proxy the cycle is broken. The reference
 */
@TestComponent
@Controller
@RequestMapping("/integration-test-public-proxy")
class ForwardingController {

  // Set by the test once Hydra's mapped public port is known; the proxy forwards browsers to
  // Hydra's public authorize endpoint (the admin base in HydraAdminClient.Properties is a
  // different endpoint for a different consumer).
  String hydraPublicBaseUri;

  @GetMapping("oauth2/auth")
  public RedirectView oauth2Auth() {
    val redirectView = new RedirectView(hydraPublicBaseUri + "/oauth2/auth");
    redirectView.setPropagateQueryParams(true);
    return redirectView;
  }

  // The logout accept's redirect_to (carrying the logout_verifier) and the post-logout
  // fallback are both issuer-based URLs, so they arrive here and forward to Hydra like
  // the authorize endpoint above.
  @GetMapping("oauth2/sessions/logout")
  public RedirectView oauth2SessionsLogout() {
    val redirectView = new RedirectView(hydraPublicBaseUri + "/oauth2/sessions/logout");
    redirectView.setPropagateQueryParams(true);
    return redirectView;
  }

  @GetMapping("oauth2/fallbacks/logout")
  public RedirectView oauth2FallbacksLogout() {
    val redirectView = new RedirectView(hydraPublicBaseUri + "/oauth2/fallbacks/logout");
    redirectView.setPropagateQueryParams(true);
    return redirectView;
  }

  @GetMapping("oauth2/fallbacks/error")
  public String oauth2FallbacksError(HttpServletRequest request) {
    return null;
  }
}

/**
 * A controller that mocks a client call back. Upon receiving a request it invokes a consumer,
 * providing a hook for tests to 1) assert that the callback was made and 2) use an argument captor
 * to access the query string. This makes it possible for a test to then parse the query string to
 * get the "code" parameter and exchange it with the auth server for the token response.
 *
 * <p>An alternative approach would be to start an entirely separate app but that seemed like more
 * hassle than is worth it.
 */
@TestComponent
@RestController
@RequestMapping(CLIENT_CALL_BACK_PATH)
class ClientCallBackController {

  public static final String CLIENT_CALL_BACK_PATH = "/client/callback";

  @Autowired Consumer<String> queryStringConsumer;

  @GetMapping
  public String oauth2Auth(HttpServletRequest request) {
    queryStringConsumer.accept(request.getQueryString());

    return "call back received: " + request.getQueryString();
  }
}
