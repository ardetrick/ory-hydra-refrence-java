package com.ardetrick.oryhydrareference;

import com.ardetrick.oryhydrareference.testcontainers.OryHydraDockerComposeContainer;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URIBuilder;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
		ForwardingController.class
})
@Testcontainers
@TestPropertySource(properties = {"debug=true"})
public class OryHydraReferenceApplicationFunctionalTests {

	@LocalServerPort
	int springBootAppPort;

	OryHydraDockerComposeContainer<?> dockerComposeEnvironment;

	OAuth2Client oAuth2Client;

	@Autowired
	OryHydraReferenceApplication.Config config;

	@BeforeEach
	public void beforeEachTest() throws ApiException {
		dockerComposeEnvironment = new OryHydraDockerComposeContainer<>(springBootAppPort);
		dockerComposeEnvironment.start();

		config.oryHydraPublicUri = dockerComposeEnvironment.publicBaseUriString();

		val oauth2Api = new OAuth2Api(
				Configuration.getDefaultApiClient()
						.setBasePath(dockerComposeEnvironment.adminBaseUriString())
		);

		val oAuth2Client = new OAuth2Client();
		oAuth2Client.clientName("test-client");
		oAuth2Client.redirectUris(List.of("http://localhost/redirect"));
		oAuth2Client.grantTypes(List.of("authorization_code"));
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

		this.oAuth2Client = oauth2Api.createOAuth2Client(oAuth2Client);

		assertThat(oauth2Api.getOAuth2Client(this.oAuth2Client.getClientId()))
				.isNotNull();
	}

	@Test
	void jwkIsExposed() throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder(URI.create(dockerComposeEnvironment.publicBaseUriString() + "/.well-known/jwks.json")).build();

		var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode())
				.isEqualTo(200);
	}

	// http://localhost:62107/oauth2/auth?response_type=code&client_id=4f7a6bbf-c0ca-4f20-acf2-42473ae95410&redirect_uri=http%3A%2F%2Flocalhost%2Fredirect&scope=offline_access+openid+offline+profile&state=12345678901234567890
	@Test
	void initialLoginRequestShouldRedirectToLoginUserInterface() throws URISyntaxException, IOException, InterruptedException {
		var uri = new URIBuilder(URI.create(dockerComposeEnvironment.publicBaseUriString() + "/oauth2/auth"))
				.addParameter("response_type", "code")
				.addParameter("client_id", oAuth2Client.getClientId())
				.addParameter("redirect_uri", Objects.requireNonNull(oAuth2Client.getRedirectUris()).get(0))
				.addParameter("scope", "offline_access openid offline profile")
				.addParameter("state", "12345678901234567890")
				.build();

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
				.contains("login_challenge");

		// start flow
		// follow IDP steps
		// exchange code for token
		// validate token
	}

	@Test
	public void playwright() {
		try (val playwright = Playwright.create();
			 val browser = playwright.webkit().launch();
		) {
			val page = browser.newPage();

			var uri = new URIBuilder(URI.create(dockerComposeEnvironment.publicBaseUriString() + "/oauth2/auth"))
					.addParameter("response_type", "code")
					.addParameter("client_id", oAuth2Client.getClientId())
					.addParameter("redirect_uri", Objects.requireNonNull(oAuth2Client.getRedirectUris()).get(0))
					.addParameter("scope", "offline_access openid offline profile")
					.addParameter("state", "12345678901234567890")
					.build();

			page.navigate(uri.toString());
			page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("build/test-results/example.png")));

			page.type("input[name=loginEmail]", "example@example.com");
			page.type("input[name=loginPassword]", "example-password");

			page.locator("input[name=submit]").click();

			synchronized(page) {
				page.wait(2_000L);
			}

			page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("build/test-results/after-submit.png")));
		} catch (URISyntaxException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}

@Controller
@RequestMapping("/integration-test-public-proxy")
class ForwardingController {

	@Autowired OryHydraReferenceApplication.Config config;

	// /integration-test-public-proxy/oauth2/auth?client_id=b06f0bc9-1a2a-412c-9749-05d179f25ba8&login_verifier=38e48b72b3d64201b9d4b1742fa8a8c9&redirect_uri=http%3A%2F%2Flocalhost%2Fredirect&response_type=code&scope=offline_access+openid+offline+profile&state=12345678901234567890
	// http://localhost:63712/integration-test-public-proxy/oauth2/auth?client_id=b960097b-8497-4f63-abec-f485349dc807&login_verifier=804a513848ce464a8b757587d5d16e9d&redirect_uri=http%3A%2F%2Flocalhost%2Fredirect&response_type=code&scope=offline_access+openid+offline+profile&state=12345678901234567890
	// http://localhost:64142/oauth2/auth
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

//@EnableWebMvc
//class WebConfig implements WebMvcConfigurer {
//
//	@Override
//	public void addViewControllers(@NotNull ViewControllerRegistry registry) {
//		registry.addViewController("/abc").setViewName();
//	}
//
//}