package com.ardetrick.oryhydrareference;

import com.ardetrick.oryhydrareference.test.utils.ScreenshotPathProducer;
import com.ardetrick.oryhydrareference.testcontainers.OryHydraDockerComposeContainer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URIBuilder;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import sh.ory.hydra.ApiException;
import sh.ory.hydra.Configuration;
import sh.ory.hydra.api.OAuth2Api;
import sh.ory.hydra.model.OAuth2Client;

import javax.servlet.http.HttpServletRequest;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
		ClientCallBackController.class,
		ForwardingController.class
})
@Testcontainers
@TestPropertySource(properties = {"debug=true"})
public class OryHydraReferenceApplicationFunctionalTests {

	@LocalServerPort int springBootAppPort;

	OryHydraDockerComposeContainer<?> dockerComposeEnvironment;

	OAuth2Client oAuth2Client;

	@Autowired OryHydraReferenceApplication.Config config;

	@MockBean Consumer<String> queryStringConsumer;
	@Captor ArgumentCaptor<String> queryStringConsumerArgumentCaptor;

	@BeforeEach
	public void beforeEachTest() throws ApiException {
		// Start Ory Hydra, passing it the port of the reference application to configure urls.
		// An alternative approach would be to start the container once and re-use it across all tests.
		// However, @BeforeAllTests requires the method be static but @LocalServerPort is unavailable statically.
		// Creating the containers for each test increases test execution time but keeps all tests isolated.
		dockerComposeEnvironment = new OryHydraDockerComposeContainer<>(springBootAppPort);
		dockerComposeEnvironment.start();

		oAuth2Client = createOAuthClient();

		// A "cheat" to break a circular dependency where the reference application needs to know the URI of Ory Hydra
		// and Ory Hydra needs to know the URI of the reference application. In a production application these two URIs
		// should be static and well known. However, in the context of these tests the ports of both the reference
		// application and Ory Hydra are randomized and are unknown until after the application is already running
		// (this follows testing best practices where hard coding ports should be avoided in case the host machine is
		// already using that port). There may be a cleaner approach out there (perhaps using Docker Networking?) but
		// in the meantime this is a low cost and sufficient work around.
		config.oryHydraPublicUri = dockerComposeEnvironment.publicBaseUriString();
	}

	@AfterEach
	public void afterEachTest() {
		// Test containers must be stopped to avoid a port conflict error when starting them up again for the next test.
		dockerComposeEnvironment.stop();
	}

	private OAuth2Client createOAuthClient() throws ApiException {
		val oAuth2Client = new OAuth2Client();
		oAuth2Client.clientName("test-client");
		oAuth2Client.redirectUris(List.of("http://localhost:" + springBootAppPort + "/client/callback"));
		oAuth2Client.grantTypes(List.of(
				"authorization_code",
				"refresh_token"
		));
		oAuth2Client.responseTypes(List.of("code", "id_token"));
		oAuth2Client.clientSecret("client-secret");
		oAuth2Client.scope(String.join(" ", "offline_access", "openid", "offline", "profile"));

		// TODO: documentation states these are optional but an error is thrown when not provided.
		oAuth2Client.authorizationCodeGrantAccessTokenLifespan("1h");
		oAuth2Client.setAuthorizationCodeGrantRefreshTokenLifespan("1h");
		oAuth2Client.authorizationCodeGrantIdTokenLifespan("1h");
		oAuth2Client.clientCredentialsGrantAccessTokenLifespan("1h");
		oAuth2Client.contacts(ImmutableList.of());
		oAuth2Client.implicitGrantAccessTokenLifespan("1h");
		oAuth2Client.implicitGrantIdTokenLifespan("1h");
		oAuth2Client.jwtBearerGrantAccessTokenLifespan("1h");
		oAuth2Client.refreshTokenGrantAccessTokenLifespan("1h");
		oAuth2Client.setRefreshTokenGrantRefreshTokenLifespan("1h");
		oAuth2Client.refreshTokenGrantIdTokenLifespan("1h");

		// Initialize API
		val oauth2Api = new OAuth2Api(
				Configuration.getDefaultApiClient().setBasePath(dockerComposeEnvironment.adminBaseUriString())
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
		val request = HttpRequest.newBuilder(getPublicJwksUri()).build();
		val response = HttpClient.newHttpClient().send(
				request,
				HttpResponse.BodyHandlers.ofString()
		);

		assertThat(response.statusCode())
				.isEqualTo(200);
	}

	private URI getPublicJwksUri() {
		return URI.create(dockerComposeEnvironment.publicBaseUriString() + "/.well-known/jwks.json");
	}

	// http://localhost:62107/oauth2/auth?response_type=code&client_id=4f7a6bbf-c0ca-4f20-acf2-42473ae95410&redirect_uri=http%3A%2F%2Flocalhost%2Fredirect&scope=offline_access+openid+offline+profile&state=12345678901234567890
	@Test
	void initialLoginRequestShouldRedirectToLoginUserInterface() throws URISyntaxException, IOException, InterruptedException {
		URI uri = getUriToInitiateFlow();

		var request = HttpRequest.newBuilder()
				.uri(uri)
				.build();

		// Note the use of `followRedirects` - Hydra will redirect this response.
		var response = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode())
				.isEqualTo(200);
		assertThat(response.uri().getPath())
				.isEqualTo("/login");
		assertThat(response.body())
				.contains("loginChallenge");

		// start flow
		// follow IDP steps
		// exchange code for token
		// validate token
	}

	@Test
	public void loginInvalidCredentials() {
		val screenshotPathProducer = ScreenshotPathProducer.builder()
				.testName("loginInvalidCredentials")
				.build();

		try (val playwright = Playwright.create();
			 val browser = playwright.webkit().launch();
		) {
			val page = browser.newPage();
			val initiateFlowUri = getUriToInitiateFlow();

			page.navigate(initiateFlowUri.toString());
			page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

			page.type("input[name=loginEmail]", "foo@bar.com");
			page.type("input[name=loginPassword]", "password1");

			page.locator("input[name=submit]").click();

			page.waitForLoadState();
			page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit.png"));

			assertThat(page.content()).contains("invalid credentials try again");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private URI getUriToInitiateFlow() throws URISyntaxException {
		return new URIBuilder(URI.create(dockerComposeEnvironment.publicBaseUriString() + "/oauth2/auth"))
				.addParameter("response_type", "code")
				.addParameter("client_id", oAuth2Client.getClientId())
				.addParameter("redirect_uri", Objects.requireNonNull(oAuth2Client.getRedirectUris()).get(0))
				.addParameter("scope", "offline_access openid offline profile")
				.addParameter("state", "12345678901234567890")
				.build();
	}

	@Test
	public void completeFullOAuthFlowUsingUIToLogin() {
		val screenshotPathProducer = ScreenshotPathProducer.builder()
				.testName("completeFullOAuthFlowUsingUIToLogin")
				.build();

		try (val playwright = Playwright.create();
			 val browser = playwright.webkit().launch();
		) {
			val page = browser.newPage();

			val uri = getUriToInitiateFlow();

			page.navigate(uri.toString());
			page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("initial-load"));

			page.type("input[name=loginEmail]", "foo@bar.com");
			page.type("input[name=loginPassword]", "password");

			page.locator("input[name=submit]").click();

			page.waitForLoadState();

			page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-login-submit.png"));

			page.locator("input[id=accept]").click();

			page.waitForLoadState();

			page.screenshot(screenshotPathProducer.screenshotOptionsForStepName("after-consent-submit.png"));

			verify(queryStringConsumer)
					.accept(queryStringConsumerArgumentCaptor.capture());

			val queryStringValue = queryStringConsumerArgumentCaptor.getValue();

			assertThat(queryStringValue).isNotBlank();

			val maybeCode = Arrays.stream(queryStringValue.split("&"))
					.filter(x -> x.startsWith("code="))
					.findFirst()
					.map(x -> x.replace("code=", ""));

			assertThat(maybeCode)
					.isNotNull()
					.isPresent();

			val params = Map.of(
							"client_id", Objects.requireNonNull(oAuth2Client.getClientId()),
							"code", maybeCode.get(),
							"grant_type", Objects.requireNonNull(oAuth2Client.getGrantTypes().get(0)),
							"redirect_uri", Objects.requireNonNull(oAuth2Client.getRedirectUris()).get(0)
					)
					.entrySet()
					.stream()
					.map(entry -> String.join("=",
							URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8),
							URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
					).collect(Collectors.joining("&"));

			val request = HttpRequest.newBuilder()
					.uri(URI.create(dockerComposeEnvironment.publicBaseUriString() + "/oauth2/token"))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.header("authorization", "Basic " + Base64.getEncoder().encodeToString((oAuth2Client.getClientId() + ":" + oAuth2Client.getClientSecret()).getBytes()))
					.POST(HttpRequest.BodyPublishers.ofString(params))
					.build();

			val codeExchangeResponse = HttpClient.newBuilder().build()
					.send(request, HttpResponse.BodyHandlers.ofString());

			assertThat(codeExchangeResponse.statusCode()).isEqualTo(200);

			val mapped = new ObjectMapper().readValue(codeExchangeResponse.body(), CodeExchangeResponse.class);

			assertThat(mapped.accessToken())
					.isNotBlank();
			assertThat(mapped.refreshToken())
					.isNotBlank();
			assertThat(mapped.idToken())
					.isNotBlank();
		} catch (URISyntaxException | InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
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
) {}

@TestComponent
@Controller
@RequestMapping("/integration-test-public-proxy")
class ForwardingController {

	@Autowired OryHydraReferenceApplication.Config config;

	@GetMapping("oauth2/auth")
	public RedirectView oauth2Auth(HttpServletRequest request) {
		val redirectView = new RedirectView(config.oryHydraPublicUri + "/oauth2/auth");
		redirectView.setPropagateQueryParams(true);
		return redirectView;
	}

	@GetMapping("oauth2/fallbacks/error")
	public String oauth2FallbacksError(HttpServletRequest request) {
		System.out.println(request.getRequestURI());
		return null;
	}

}

@TestComponent
@RestController
@RequestMapping("/client/callback")
class ClientCallBackController {

	@Autowired Consumer<String> queryStringConsumer;

	@GetMapping
	public String oauth2Auth(HttpServletRequest request) {
		queryStringConsumer.accept(request.getQueryString());

		return "call back received: " + request.getQueryString();
	}

}
