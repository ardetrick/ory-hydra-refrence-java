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

  OryHydraContainer dockerComposeEnvironment;

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
    dockerComposeEnvironment =
        OryHydraContainer.builder()
            .urlsLogin("http://localhost:" + springBootAppPort + "/login")
            .urlsConsent("http://localhost:" + springBootAppPort + "/consent")
            .urlsSelfIssuer(
                "http://localhost:" + springBootAppPort + "/integration-test-public-proxy")
            .build();
    dockerComposeEnvironment.start();

    // Late-bind the two Hydra endpoints now that the mapped ports exist (the circular
    // port dependency: the app and Hydra each need the other's randomized URI). Two consumers,
    // two different endpoints: the app's HydraAdminClient speaks the admin API, while the
    // test's public proxy forwards browsers to the public authorize endpoint.
    properties.setBasePath(dockerComposeEnvironment.adminBaseUriString());
    forwardingController.hydraPublicBaseUri = dockerComposeEnvironment.publicBaseUriString();
  }

  @AfterAll
  void stopTestEnvironment() {
    playwright.close();
    dockerComposeEnvironment.stop();
  }

  @BeforeEach
  public void registerTestClient() {
    // A unique client per test keeps tests isolated on the shared container: Hydra remembers
    // consent per subject + client, so a shared client would leak remembered consent between
    // tests. Registration is a cheap upserting admin API call via the container.
    clientId = "test-client-" + UUID.randomUUID();
    clientSecret = "client-secret";
    redirectUri = "http://localhost:" + springBootAppPort + CLIENT_CALL_BACK_PATH;
    dockerComposeEnvironment.createOrReplaceClient(
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
                URI.create(
                    dockerComposeEnvironment.publicBaseUriString() + "/.well-known/jwks.json"))
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
      return new URIBuilder(dockerComposeEnvironment.publicBaseUriString() + "/oauth2/auth")
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
            .uri(URI.create(dockerComposeEnvironment.publicBaseUriString() + "/oauth2/token"))
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
