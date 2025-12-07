package com.ardetrick.oryhydrareference;

import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import com.ardetrick.oryhydrareference.test.utils.ScreenshotPathProducer;
import com.ardetrick.testcontainers.OryHydraComposeContainer;
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
import org.testcontainers.junit.jupiter.Testcontainers;
import sh.ory.hydra.ApiException;
import sh.ory.hydra.Configuration;
import sh.ory.hydra.api.OAuth2Api;
import sh.ory.hydra.model.OAuth2Client;

import java.io.File;
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

import static com.ardetrick.oryhydrareference.ClientCallBackController.CLIENT_CALL_BACK_PATH;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        ClientCallBackController.class,
        ForwardingController.class,
})
@Testcontainers
@TestPropertySource(properties = {"debug=true"})
public class OryHydraReferenceApplicationFunctionalTests {

    // Shared between all tests in this class.
    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    int springBootAppPort;

    OryHydraComposeContainer dockerComposeEnvironment;

    OAuth2Client oAuth2Client;

    @Autowired
    HydraAdminClient.Properties properties;

    @MockitoBean
    Consumer<String> queryStringConsumer;
    @Captor
    ArgumentCaptor<String> queryStringConsumerArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeAll
    static void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium()
                            .launch();
    }

    @AfterAll
    static void afterAll() {
        playwright.close();
    }

    @BeforeEach
    public void beforeEachTest() throws ApiException {
        // Start Ory Hydra, passing it the port of the reference application to configure urls.
        // An alternative approach would be to start the container once and re-use it across all tests.
        // However, @BeforeAllTests requires the method be static but @LocalServerPort is unavailable statically.
        // Creating the containers for each test increases test execution time but keeps all tests isolated.
        dockerComposeEnvironment = OryHydraComposeContainer.builder()
                                                           .dockerComposeFile(new File("src/test/resources/docker-compose.yml"))
                                                           .urlsLogin("http://localhost:" + springBootAppPort + "/login")
                                                           .urlsConsent("http://localhost:" + springBootAppPort + "/consent")
                                                           .urlsSelfIssuer("http://localhost:" + springBootAppPort + "/integration-test-public-proxy")
                                                           .start();

        // A "cheat" to break a circular dependency where the reference application needs to know the URI of Ory Hydra
        // and Ory Hydra needs to know the URI of the reference application. In a production application these two URIs
        // should be static and well known. However, in the context of these tests the ports of both the reference
        // application and Ory Hydra are randomized and are unknown until after the application is already running
        // (this follows testing best practices where hard coding ports should be avoided in case the host machine is
        // already using that port). There may be a cleaner approach out there (perhaps using Docker Networking?) but
        // in the meantime this is a low cost and sufficient work around.
        properties.setBasePath(dockerComposeEnvironment.publicBaseUriString());

        oAuth2Client = createOAuthClient();
    }

    @AfterEach
    public void afterEachTest() {
        // Test containers must be stopped to avoid a port conflict error when starting them up again for the next test.
        dockerComposeEnvironment.stop();
    }

    private OAuth2Client createOAuthClient() throws ApiException {
        val oAuth2Client = new OAuth2Client();
        oAuth2Client.clientName("test-client");
        oAuth2Client.redirectUris(List.of("http://localhost:" + springBootAppPort + CLIENT_CALL_BACK_PATH));
        oAuth2Client.grantTypes(List.of(
                "authorization_code",
                "refresh_token"
        ));
        oAuth2Client.responseTypes(List.of("code", "id_token"));
        oAuth2Client.clientSecret("client-secret");
        oAuth2Client.scope(String.join(" ", "offline_access", "openid", "offline", "profile"));

        // Initialize API
        val oauth2Api = new OAuth2Api(
                Configuration.getDefaultApiClient()
                             .setBasePath(dockerComposeEnvironment.adminBaseUriString())
        );

        // Create client
        val oauth2Client = oauth2Api.createOAuth2Client(oAuth2Client);

        // Basic assertions on created client
        assertThat(oauth2Api.getOAuth2Client(oauth2Client.getClientId()))
                .isNotNull();

        return oauth2Client;
    }

    /**
     * Tests that the .well-known/jwks.json URI is exposed and accessible.
     *
     * @link <a href="https://www.ory.sh/docs/reference/api#tag/wellknown/operation/discoverJsonWebKeys"/>
     */
    @Test
    void requestToJwksUriReturns200() throws IOException, InterruptedException {
        val request = HttpRequest.newBuilder(dockerComposeEnvironment.getPublicJwksUri())
                                 .build();
        val response = HttpClient.newHttpClient()
                                 .send(
                                         request,
                                         HttpResponse.BodyHandlers.ofString()
                                 );

        assertThat(response.statusCode())
                .isEqualTo(200);
    }

    @Test
    public void loginInvalidCredentials() {
        val screenshotPathProducer = ScreenshotPathProducer.builder()
                                                           .testName("loginInvalidCredentials")
                                                           .build();

        val page = browser.newPage();
        val initiateFlowUri = getUriToInitiateFlow();

        page.navigate(initiateFlowUri.toString());
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

        page.locator("input[name=loginEmail]")
            .fill("foo@bar.com");
        page.locator("input[name=loginPassword]")
            .fill("password1");

        page.locator("input[name=submit]")
            .click();

        page.waitForLoadState();
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

        assertThat(page.content()).contains("invalid credentials try again");
    }

    private URI getUriToInitiateFlow() {
        try {
            return new URIBuilder(dockerComposeEnvironment.getOAuth2AuthUri())
                    .addParameter("response_type", "code")
                    .addParameter("client_id", oAuth2Client.getClientId())
                    .addParameter("redirect_uri",
                                  Objects.requireNonNull(oAuth2Client.getRedirectUris())
                                         .get(0))
                    .addParameter("scope", "offline_access openid offline profile")
                    .addParameter("state", "12345678901234567890")
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void completeFullOAuthFlowUsingUIToLogin() {
        val screenshotPathProducer = ScreenshotPathProducer.builder()
                                                           .testName("completeFullOAuthFlowUsingUIToLogin")
                                                           .build();

        val page = browser.newPage();

        val uri = getUriToInitiateFlow();

        page.navigate(uri.toString());
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

        page.locator("input[name=loginEmail]")
            .fill("foo@bar.com");
        page.locator("input[name=loginPassword]")
            .fill("password");

        page.locator("input[name=submit]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

        page.locator("input[id=accept]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

        val code = getCodeFromCallbackCaptor();
        val token = exchangeCode(code);

        assertThat(token.accessToken())
                .isNotBlank();
        assertThat(token.refreshToken())
                .isNotBlank();
        assertThat(token.idToken())
                .isNotBlank();

        val decodedJWT = JWT.decode(token.idToken());
        assertThat(decodedJWT.getClaim("exampleCustomClaimKey")
                             .asString())
                .isNotNull()
                .isEqualTo("example custom claim value");
    }

    @Test
    public void completeFlowWithPartialScopeSelection() {
        val screenshotPathProducer = ScreenshotPathProducer.builder()
                                                           .testName("completeFlowWithPartialScopeSelection")
                                                           .build();

        val page = browser.newPage();

        val uri = getUriToInitiateFlow();

        page.navigate(uri.toString());
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

        page.locator("input[name=loginEmail]")
            .fill("foo@bar.com");
        page.locator("input[name=loginPassword]")
            .fill("password");

        page.locator("input[name=submit]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

        page.locator("input[id=scopes-profile]")
            .uncheck();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-uncheck-scope"));

        page.locator("input[id=accept]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

        val code = getCodeFromCallbackCaptor();
        val token = exchangeCode(code);

        assertThat(token.accessToken())
                .isNotBlank();
        assertThat(token.refreshToken())
                .isNotBlank();
        assertThat(token.idToken())
                .isNotBlank();
        assertThat(token.scope()).doesNotContain("profile");
    }

    private CodeExchangeResponse exchangeCode(String code) {
        val encodedParams = Map.of(
                                       "client_id",
                                       Objects.requireNonNull(oAuth2Client.getClientId()),
                                       "code",
                                       code,
                                       "grant_type",
                                       Objects.requireNonNull(Objects.requireNonNull(oAuth2Client.getGrantTypes())
                                                                     .get(0)),
                                       "redirect_uri",
                                       Objects.requireNonNull(oAuth2Client.getRedirectUris())
                                              .get(0)
                               )
                               .entrySet()
                               .stream()
                               .map(entry -> String.join("=",
                                                         URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8),
                                                         URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                               )
                               .collect(Collectors.joining("&"));

        val request = HttpRequest.newBuilder()
                                 .uri(URI.create(dockerComposeEnvironment.publicBaseUriString() + "/oauth2/token"))
                                 .header("Content-Type", "application/x-www-form-urlencoded")
                                 .header("authorization",
                                         "Basic " + Base64.getEncoder()
                                                          .encodeToString((oAuth2Client.getClientId() + ":" + oAuth2Client.getClientSecret()).getBytes()))
                                 .POST(HttpRequest.BodyPublishers.ofString(encodedParams))
                                 .build();

        HttpResponse<String> codeExchangeResponse;
        try {
            codeExchangeResponse = HttpClient.newBuilder()
                                             .build()
                                             .send(request, HttpResponse.BodyHandlers.ofString());
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
        val screenshotPathProducer = ScreenshotPathProducer.builder()
                                                           .testName("skipConsentScreenOnSecondLoginWhenRememberMeIsUsed")
                                                           .build();
        val page = browser.newPage();

        val uri = getUriToInitiateFlow();

        page.navigate(uri.toString());
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

        page.locator("input[name=loginEmail]")
            .fill("foo@bar.com");
        page.locator("input[name=loginPassword]")
            .fill("password");

        page.locator("input[name=submit]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

        page.locator("input[id=accept]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

        val code = getCodeFromCallbackCaptor();
        exchangeCode(code);

        page.navigate(getUriToInitiateFlow().toString());
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load-second-time"));

        page.locator("input[name=loginEmail]")
            .fill("foo@bar.com");
        page.locator("input[name=loginPassword]")
            .fill("password");

        page.locator("input[name=submit]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit-second-time"));

        // Consent screen is skipped
        assertThat(page.url()).contains("/client/callback?code=");
    }

    private String getCodeFromCallbackCaptor() {
        verify(queryStringConsumer)
                .accept(queryStringConsumerArgumentCaptor.capture());
        val queryStringValue = queryStringConsumerArgumentCaptor.getValue();
        return Arrays.stream(queryStringValue.split("&"))
                     .filter(queryStringParam -> queryStringParam.startsWith("code="))
                     .findFirst()
                     .map(queryStringParam -> queryStringParam.replace("code=", ""))
                     .orElseThrow();
    }

    @Test
    public void doNotSkipConsentScreenOnSecondLoginWhenRememberMeIsFalse() {
        val screenshotPathProducer = ScreenshotPathProducer.builder()
                                                           .testName("doNotSkipConsentScreenOnSecondLoginWhenRememberMeIsFalse")
                                                           .build();

        val page = browser.newPage();

        val uri = getUriToInitiateFlow();

        page.navigate(uri.toString());
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

        page.locator("input[name=loginEmail]")
            .fill("foo@bar.com");
        page.locator("input[name=loginPassword]")
            .fill("password");

        page.locator("input[name=submit]")
            .click();

        page.waitForLoadState();
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit"));

        page.locator("input[id=remember]")
            .uncheck();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-uncheck"));

        page.locator("input[id=accept]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit"));

        val code = getCodeFromCallbackCaptor();
        exchangeCode(code);

        page.navigate(getUriToInitiateFlow().toString());
        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load-second-time"));

        page.locator("input[name=loginEmail]")
            .fill("foo@bar.com");
        page.locator("input[name=loginPassword]")
            .fill("password");

        page.locator("input[name=submit]")
            .click();

        page.waitForLoadState();

        page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit-second-time"));

        // Consent screen is not skipped
        assertThat(page.url()).contains("/consent");
    }

}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record CodeExchangeResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("scope") String scope,
        @JsonProperty("token_type") String tokenType
) {
}

/**
 * A controller that exposes an endpoint which acts as a proxy to the oauth server. This proxy is used to break the
 * unfortunate circular dependency between Hydra and the reference app. They each need to know about each other at
 * startup, but within the context of testing both are using random ports, so it is impossible for each to know
 * about each other until after the ports have been assigned. By using this proxy the cycle is broken. The reference
 */
@TestComponent
@Controller
@RequestMapping("/integration-test-public-proxy")
class ForwardingController {

    @Autowired
    HydraAdminClient.Properties properties;

    @GetMapping("oauth2/auth")
    public RedirectView oauth2Auth() {
        val redirectView = new RedirectView(properties.getBasePath() + "/oauth2/auth");
        redirectView.setPropagateQueryParams(true);
        return redirectView;
    }

    @GetMapping("oauth2/fallbacks/error")
    public String oauth2FallbacksError(HttpServletRequest request) {
        return null;
    }

}

/**
 * A controller that mocks a client call back. Upon receiving a request it invokes a consumer, providing a hook for
 * tests to 1) assert that the callback was made and 2) use an argument captor to access the query string. This makes it
 * possible for a test to then parse the query string to get the "code" parameter and exchange it with the auth server
 * for the token response.
 * <p>
 * An alternative approach would be to start an entirely separate app but that seemed like more hassle than is worth it.
 */
@TestComponent
@RestController
@RequestMapping(CLIENT_CALL_BACK_PATH)
class ClientCallBackController {

    public static final String CLIENT_CALL_BACK_PATH = "/client/callback";

    @Autowired
    Consumer<String> queryStringConsumer;

    @GetMapping
    public String oauth2Auth(HttpServletRequest request) {
        queryStringConsumer.accept(request.getQueryString());

        return "call back received: " + request.getQueryString();
    }

}
