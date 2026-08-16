package com.example.demo.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private final GoogleIdTokenVerifier verifier;
    private final String clientId;

    public GoogleTokenVerifier(@Value("${GOOGLE_CLIENT_ID:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId;
        if (this.clientId.isBlank()) {
            logger.warn("GOOGLE_CLIENT_ID not set — Google token verification will be disabled (dev fallback)");
            this.verifier = null;
        } else {
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                    .setAudience(Collections.singletonList(this.clientId))
                    .build();
        }
    }

    /**
     * Verify the given idToken string and return the GoogleIdToken if valid, otherwise null.
     */
    public boolean isEnabled() {
        return this.verifier != null;
    }

    public GoogleIdToken verify(String idTokenString) {
        if (verifier == null) {
            // verification disabled — treat as stub
            logger.info("Google verification disabled; demo fallback for token: {}", idTokenString == null ? "(null)" : idTokenString.substring(0, Math.min(8, idTokenString.length())));
            return null;
        }
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken;
            } else {
                logger.warn("Invalid Google id_token");
                return null;
            }
        } catch (GeneralSecurityException | IOException e) {
            logger.warn("Failed to verify Google id_token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            // Google library can throw IllegalArgumentException for malformed tokens; treat as invalid
            logger.warn("Google id_token verification error: {}", e.getMessage());
            return null;
        }
    }
}
