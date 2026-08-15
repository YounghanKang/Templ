package com.example.demo.integration.github;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GitHubWebhookControllerTest {

    @Test
    void invalidSignatureReturnsUnauthorized() {

        GitHubWebhookSignatureVerifier signatureVerifier =
                mock(
                        GitHubWebhookSignatureVerifier.class
                );

        GitHubWebhookService webhookService =
                mock(
                        GitHubWebhookService.class
                );


        GitHubWebhookController controller =
                new GitHubWebhookController(
                        signatureVerifier,
                        webhookService
                );


        UUID projectId =
                UUID.randomUUID();

        byte[] payload =
                "{}".getBytes(
                        StandardCharsets.UTF_8
                );

        String signature =
                "sha256=invalid";


        when(
                signatureVerifier.isValid(
                        payload,
                        signature
                )
        ).thenReturn(
                false
        );


        ResponseEntity<Void> response =
                controller.receiveWebhook(
                        projectId,
                        "push",
                        "delivery-001",
                        signature,
                        payload
                );


        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );


        verifyNoInteractions(
                webhookService
        );
    }


    @Test
    void validPushWebhookIsForwardedToService() {

        GitHubWebhookSignatureVerifier signatureVerifier =
                mock(
                        GitHubWebhookSignatureVerifier.class
                );

        GitHubWebhookService webhookService =
                mock(
                        GitHubWebhookService.class
                );


        GitHubWebhookController controller =
                new GitHubWebhookController(
                        signatureVerifier,
                        webhookService
                );


        UUID projectId =
                UUID.randomUUID();

        byte[] payload =
                """
                {
                  "ref": "refs/heads/main"
                }
                """.getBytes(
                        StandardCharsets.UTF_8
                );

        String deliveryId =
                "delivery-002";

        String signature =
                "sha256=valid";


        when(
                signatureVerifier.isValid(
                        payload,
                        signature
                )
        ).thenReturn(
                true
        );


        ResponseEntity<Void> response =
                controller.receiveWebhook(
                        projectId,
                        "push",
                        deliveryId,
                        signature,
                        payload
                );


        assertEquals(
                HttpStatus.ACCEPTED,
                response.getStatusCode()
        );


        verify(
                webhookService,
                times(1)
        ).handlePush(
                projectId,
                deliveryId,
                payload
        );
    }
    @Test
    void validIssueWebhookIsForwardedToService() {

        GitHubWebhookSignatureVerifier signatureVerifier =
                mock(
                        GitHubWebhookSignatureVerifier.class
                );

        GitHubWebhookService webhookService =
                mock(
                        GitHubWebhookService.class
                );


        GitHubWebhookController controller =
                new GitHubWebhookController(
                        signatureVerifier,
                        webhookService
                );


        UUID projectId =
                UUID.randomUUID();

        byte[] payload =
                """
                {
                  "action": "opened",
                  "issue": {
                    "number": 17
                  }
                }
                """.getBytes(
                        StandardCharsets.UTF_8
                );

        String deliveryId =
                "delivery-issue-001";

        String signature =
                "sha256=valid";


        when(
                signatureVerifier.isValid(
                        payload,
                        signature
                )
        ).thenReturn(
                true
        );


        ResponseEntity<Void> response =
                controller.receiveWebhook(
                        projectId,
                        "issues",
                        deliveryId,
                        signature,
                        payload
                );


        assertEquals(
                HttpStatus.ACCEPTED,
                response.getStatusCode()
        );


        verify(
                webhookService,
                times(1)
        ).handleIssue(
                projectId,
                deliveryId,
                payload
        );


        verify(
                webhookService,
                never()
        ).handlePush(
                any(),
                any(),
                any()
        );
    }
    @Test
    void validPullRequestWebhookIsForwardedToService() {

        GitHubWebhookSignatureVerifier signatureVerifier =
                mock(
                        GitHubWebhookSignatureVerifier.class
                );

        GitHubWebhookService webhookService =
                mock(
                        GitHubWebhookService.class
                );


        GitHubWebhookController controller =
                new GitHubWebhookController(
                        signatureVerifier,
                        webhookService
                );


        UUID projectId =
                UUID.randomUUID();

        byte[] payload =
                """
                {
                  "action": "opened",
                  "number": 25,
                  "pull_request": {
                    "title": "로그인 인증 구조 변경"
                  }
                }
                """.getBytes(
                        StandardCharsets.UTF_8
                );

        String deliveryId =
                "delivery-pr-001";

        String signature =
                "sha256=valid";


        when(
                signatureVerifier.isValid(
                        payload,
                        signature
                )
        ).thenReturn(
                true
        );


        ResponseEntity<Void> response =
                controller.receiveWebhook(
                        projectId,
                        "pull_request",
                        deliveryId,
                        signature,
                        payload
                );


        assertEquals(
                HttpStatus.ACCEPTED,
                response.getStatusCode()
        );


        verify(
                webhookService,
                times(1)
        ).handlePullRequest(
                projectId,
                deliveryId,
                payload
        );


        verify(
                webhookService,
                never()
        ).handlePush(
                any(),
                any(),
                any()
        );


        verify(
                webhookService,
                never()
        ).handleIssue(
                any(),
                any(),
                any()
        );
    }
}