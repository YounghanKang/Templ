package com.example.demo.integration.github;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubWebhookSignatureVerifierTest {

    private static final String SECRET =
            "It's a Secret to Everybody";

    private static final String PAYLOAD =
            "Hello, World!";

    private static final String VALID_SIGNATURE =
            "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";


    @Test
    void validGitHubSignatureIsAccepted() {

        GitHubWebhookSignatureVerifier verifier =
                new GitHubWebhookSignatureVerifier(
                        SECRET
                );


        boolean result =
                verifier.isValid(
                        PAYLOAD.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        VALID_SIGNATURE
                );


        assertTrue(result);
    }


    @Test
    void modifiedPayloadIsRejected() {

        GitHubWebhookSignatureVerifier verifier =
                new GitHubWebhookSignatureVerifier(
                        SECRET
                );


        boolean result =
                verifier.isValid(
                        "Hello, Modified!".getBytes(
                                StandardCharsets.UTF_8
                        ),
                        VALID_SIGNATURE
                );


        assertFalse(result);
    }


    @Test
    void missingSignatureIsRejected() {

        GitHubWebhookSignatureVerifier verifier =
                new GitHubWebhookSignatureVerifier(
                        SECRET
                );


        boolean result =
                verifier.isValid(
                        PAYLOAD.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        null
                );


        assertFalse(result);
    }
}