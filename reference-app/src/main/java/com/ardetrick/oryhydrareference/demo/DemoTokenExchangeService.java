package com.ardetrick.oryhydrareference.demo;

import com.ardetrick.oryhydrareference.hydra.HydraAdminClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * Performs the authorization code exchange (RFC 6749 section 4.1.3) for the demo callback page. In
 * a real deployment this request is made by the client application's backend; it lives here only so
 * the demo flow can finish in the browser. The raw response is returned pretty-printed so the page
 * can show exactly what the token endpoint said.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DemoTokenExchangeService {

  static ObjectMapper objectMapper = new ObjectMapper();

  @NonNull HydraAdminClient.Properties properties;

  public Result exchange(@NonNull final ExchangeForm exchangeForm) {
    val form =
        "grant_type=authorization_code"
            + "&code="
            + URLEncoder.encode(exchangeForm.code(), StandardCharsets.UTF_8)
            + "&redirect_uri="
            + URLEncoder.encode(exchangeForm.redirectUri(), StandardCharsets.UTF_8)
            + "&client_id="
            + URLEncoder.encode(exchangeForm.clientId(), StandardCharsets.UTF_8);
    val basicAuth =
        Base64.getEncoder()
            .encodeToString(
                (exchangeForm.clientId() + ":" + exchangeForm.clientSecret())
                    .getBytes(StandardCharsets.UTF_8));
    val request =
        HttpRequest.newBuilder(URI.create(properties.getPublicBasePath() + "/oauth2/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + basicAuth)
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();

    try {
      val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      val json = objectMapper.readTree(response.body());
      val pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
      val idTokenClaims =
          json.hasNonNull("id_token") ? decodeJwtPayload(json.get("id_token").asText()) : null;
      return new Result(pretty, idTokenClaims);
    } catch (IOException e) {
      throw new RuntimeException("Token exchange failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Token exchange interrupted", e);
    }
  }

  // Split-and-decode is enough to *display* claims; verifying the signature against the JWKS is a
  // client responsibility deliberately not demoed here.
  private static String decodeJwtPayload(String jwt) throws JsonProcessingException {
    val parts = jwt.split("\\.");
    if (parts.length < 2) {
      return null;
    }
    val payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    return objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(objectMapper.readTree(payload));
  }

  public record Result(String prettyTokenResponse, String prettyIdTokenClaims) {}
}
