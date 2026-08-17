package com.example.demo.integration.github;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/integrations/github")
public class GitHubWebhookController {

    private final GitHubWebhookSignatureVerifier signatureVerifier;

    private final GitHubWebhookService webhookService;


    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(
            @PathVariable String projectId,
            @RequestHeader("X-GitHub-Event") String githubEvent,
            @RequestHeader("X-GitHub-Delivery") String deliveryId,
            @RequestHeader(
                    value = "X-Hub-Signature-256",
                    required = false
            )
            String signature,
            @RequestBody byte[] payload
    ) {

        if (
                !signatureVerifier.isValid(
                        payload,
                        signature
                )
        ) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .build();
        }




        if (
                "push".equals(
                        githubEvent
                )
        ) {

            webhookService.handlePush(
                    projectId,
                    deliveryId,
                    payload
            );


            return ResponseEntity
                    .accepted()
                    .build();
        }

        if (
                "issues".equals(
                        githubEvent
                )
        ) {

            webhookService.handleIssue(
                    projectId,
                    deliveryId,
                    payload
            );


            return ResponseEntity
                    .accepted()
                    .build();
        }
        if (
                "pull_request".equals(
                        githubEvent
                )
        ) {

            webhookService.handlePullRequest(
                    projectId,
                    deliveryId,
                    payload
            );


            return ResponseEntity
                    .accepted()
                    .build();
        }


        return ResponseEntity
                .accepted()
                .build();
    }
}