package com.featureforge.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.featureforge.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies a Google "Sign in with Google" ID token by calling Google's
 * tokeninfo endpoint — this validates the signature, expiry, and issuer
 * server-side in one call, so FeatureForge never has to fetch or cache
 * Google's public JWKs itself.
 *
 * Tradeoff: the tokeninfo endpoint isn't recommended for high-QPS production
 * use (Google rate-limits it) — a high-volume system would instead verify
 * the JWT signature locally against Google's published JWKs (e.g. via
 * google-api-client's GoogleIdTokenVerifier) and cache the keys. For this
 * project's login QPS, the extra dependency isn't justified; one HTTPS call
 * per login is the simpler, still-correct choice.
 */
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    @Value("${featureforge.oauth.google.client-id:}")
    private String googleClientId;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper;

    public record GoogleIdentity(String email, boolean emailVerified, String name) {
    }

    public GoogleIdentity verify(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException(
                    "featureforge.oauth.google.client-id is not configured — set GOOGLE_CLIENT_ID");
        }

        JsonNode payload = fetchTokenInfo(idToken);

        String audience = payload.path("aud").asText("");
        if (!googleClientId.equals(audience)) {
            throw new InvalidCredentialsException();
        }

        String issuer = payload.path("iss").asText("");
        if (!issuer.equals("accounts.google.com") && !issuer.equals("https://accounts.google.com")) {
            throw new InvalidCredentialsException();
        }

        boolean emailVerified = payload.path("email_verified").asBoolean(false);
        String email = payload.path("email").asText(null);
        String name = payload.path("name").asText(email);

        if (email == null) {
            throw new InvalidCredentialsException();
        }

        return new GoogleIdentity(email, emailVerified, name);
    }

    private JsonNode fetchTokenInfo(String idToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKENINFO_URL + idToken))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new InvalidCredentialsException();
            }

            return objectMapper.readTree(response.body());
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCredentialsException();
        }
    }
}
