package com.example.demo.integration.github;

import com.example.demo.collaboration.CollaborationEventIngestionService;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertFalse;

class GitHubWebhookServiceTest {

    @Test
    void pushEventIsConvertedAndIngested() {

        CollaborationEventIngestionService ingestionService =
                mock(
                        CollaborationEventIngestionService.class
                );


        JsonMapper jsonMapper =
                JsonMapper
                        .builder()
                        .build();


        GitHubWebhookService service =
                new GitHubWebhookService(
                        jsonMapper,
                        org.mockito.Mockito.mock(GitHubCompareClient.class),
                        ingestionService
                );


        UUID projectId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );

        String deliveryId =
                "github-delivery-001";


        String payload =
                """
                {
                  "ref": "refs/heads/main",
                  "before": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "after": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "created": false,
                  "deleted": false,
                  "forced": false,
                  "compare": "https://github.com/YounghanKang/Templ/compare/aaa...bbb",
                  "repository": {
                    "full_name": "YounghanKang/Templ"
                  },
                  "sender": {
                    "id": 12345,
                    "login": "octocat"
                  },
                  "head_commit": {
                    "timestamp": "2026-08-15T14:30:00Z"
                  },
                  "commits": [
                    {
                      "id": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                      "message": "feat: implement github webhook ingestion"
                    }
                  ]
                }
                """;


        service.handlePush(
                projectId,
                deliveryId,
                payload.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        ArgumentCaptor<String> contentCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );


        verify(
                ingestionService
        ).ingest(
                eq(projectId),
                eq(SourceTool.GITHUB),
                eq(CollaborationEventType.PUSH),
                eq(EventAction.UPDATED),
                eq(deliveryId),
                eq("GitHub push: refs/heads/main"),
                contentCaptor.capture(),
                eq("12345"),
                eq("octocat"),
                eq("YounghanKang/Templ:refs/heads/main"),
                eq("https://github.com/YounghanKang/Templ/compare/aaa...bbb"),
                eq(false),
                eq(
                        Instant.parse(
                                "2026-08-15T14:30:00Z"
                        )
                )
        );


        String content =
                contentCaptor.getValue();


        assertTrue(
                content.contains(
                        "ref: refs/heads/main"
                )
        );

        assertTrue(
                content.contains(
                        "before: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                )
        );

        assertTrue(
                content.contains(
                        "after: bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                )
        );

        assertTrue(
                content.contains(
                        "bbbbbbb feat: implement github webhook ingestion"
                )
        );
    }
    @Test
    void pushEventIncludesCompareChangedFiles() {

        CollaborationEventIngestionService ingestionService =
                mock(
                        CollaborationEventIngestionService.class
                );

        GitHubCompareClient compareClient =
                mock(
                        GitHubCompareClient.class
                );


        JsonMapper jsonMapper =
                JsonMapper
                        .builder()
                        .build();


        GitHubWebhookService service =
                new GitHubWebhookService(
                        jsonMapper,
                        compareClient,
                        ingestionService
                );


        UUID projectId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );

        String deliveryId =
                "github-delivery-compare-001";


        when(
                compareClient.compare(
                        "YounghanKang/Templ",
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                )
        ).thenReturn(
                new GitHubCompareClient.CompareResult(
                        List.of(
                                new GitHubCompareClient.ChangedFile(
                                        "src/main/java/com/example/demo/AuthService.java",
                                        "modified",
                                        12,
                                        3,
                                        15,
                                        "@@ -1 +1 @@\n-old auth\n+new auth"
                                )
                        )
                )
        );


        String payload =
                """
                {
                  "ref": "refs/heads/main",
                  "before": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "after": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "created": false,
                  "deleted": false,
                  "forced": false,
                  "compare": "https://github.com/YounghanKang/Templ/compare/aaa...bbb",
                  "repository": {
                    "full_name": "YounghanKang/Templ"
                  },
                  "sender": {
                    "id": 12345,
                    "login": "octocat"
                  },
                  "head_commit": {
                    "timestamp": "2026-08-15T14:30:00Z"
                  },
                  "commits": [
                    {
                      "id": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                      "message": "feat: change authentication implementation"
                    }
                  ]
                }
                """;


        service.handlePush(
                projectId,
                deliveryId,
                payload.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        ArgumentCaptor<String> contentCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );


        verify(
                ingestionService
        ).ingest(
                eq(projectId),
                eq(SourceTool.GITHUB),
                eq(CollaborationEventType.PUSH),
                eq(EventAction.UPDATED),
                eq(deliveryId),
                eq("GitHub push: refs/heads/main"),
                contentCaptor.capture(),
                eq("12345"),
                eq("octocat"),
                eq("YounghanKang/Templ:refs/heads/main"),
                eq("https://github.com/YounghanKang/Templ/compare/aaa...bbb"),
                eq(false),
                eq(
                        Instant.parse(
                                "2026-08-15T14:30:00Z"
                        )
                )
        );


        String content =
                contentCaptor.getValue();


        assertTrue(
                content.contains(
                        "changed files:"
                )
        );

        assertTrue(
                content.contains(
                        "src/main/java/com/example/demo/AuthService.java"
                )
        );

        assertTrue(
                content.contains(
                        "[modified] +12 -3 (15 changes)"
                )
        );

        assertTrue(
                content.contains(
                        "-old auth"
                )
        );

        assertTrue(
                content.contains(
                        "+new auth"
                )
        );


        verify(
                compareClient
        ).compare(
                "YounghanKang/Templ",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        );
    }
    @Test
    void pushEventIsStillIngestedWhenCompareFails() {

        CollaborationEventIngestionService ingestionService =
                mock(
                        CollaborationEventIngestionService.class
                );

        GitHubCompareClient compareClient =
                mock(
                        GitHubCompareClient.class
                );


        JsonMapper jsonMapper =
                JsonMapper
                        .builder()
                        .build();


        GitHubWebhookService service =
                new GitHubWebhookService(
                        jsonMapper,
                        compareClient,
                        ingestionService
                );


        UUID projectId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );

        String deliveryId =
                "github-delivery-compare-fail-001";


        when(
                compareClient.compare(
                        "YounghanKang/Templ",
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                )
        ).thenThrow(
                new RuntimeException(
                        "GitHub Compare API failure"
                )
        );


        String payload =
                """
                {
                  "ref": "refs/heads/main",
                  "before": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "after": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "created": false,
                  "deleted": false,
                  "forced": false,
                  "compare": "https://github.com/YounghanKang/Templ/compare/aaa...bbb",
                  "repository": {
                    "full_name": "YounghanKang/Templ"
                  },
                  "sender": {
                    "id": 12345,
                    "login": "octocat"
                  },
                  "head_commit": {
                    "timestamp": "2026-08-15T14:30:00Z"
                  },
                  "commits": [
                    {
                      "id": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                      "message": "feat: update authentication"
                    }
                  ]
                }
                """;


        service.handlePush(
                projectId,
                deliveryId,
                payload.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        ArgumentCaptor<String> contentCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );


        verify(
                ingestionService
        ).ingest(
                eq(projectId),
                eq(SourceTool.GITHUB),
                eq(CollaborationEventType.PUSH),
                eq(EventAction.UPDATED),
                eq(deliveryId),
                eq("GitHub push: refs/heads/main"),
                contentCaptor.capture(),
                eq("12345"),
                eq("octocat"),
                eq("YounghanKang/Templ:refs/heads/main"),
                eq("https://github.com/YounghanKang/Templ/compare/aaa...bbb"),
                eq(false),
                eq(
                        Instant.parse(
                                "2026-08-15T14:30:00Z"
                        )
                )
        );


        String content =
                contentCaptor.getValue();


        assertTrue(
                content.contains(
                        "feat: update authentication"
                )
        );

        assertFalse(
                content.contains(
                        "changed files:"
                )
        );


        verify(
                compareClient
        ).compare(
                "YounghanKang/Templ",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        );
    }
    @Test
    void issueOpenedEventIsConvertedAndIngested() {

        CollaborationEventIngestionService ingestionService =
                mock(
                        CollaborationEventIngestionService.class
                );


        JsonMapper jsonMapper =
                JsonMapper
                        .builder()
                        .build();


        GitHubWebhookService service =
                new GitHubWebhookService(
                        jsonMapper,
                        org.mockito.Mockito.mock(GitHubCompareClient.class),
                        ingestionService
                );


        UUID projectId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );

        String deliveryId =
                "github-delivery-issue-001";


        String payload =
                """
                {
                  "action": "opened",
                  "issue": {
                    "number": 17,
                    "title": "로그인 API 구현 방향 변경",
                    "body": "JWT 대신 세션 인증 방식으로 변경하는 것을 제안합니다.",
                    "state": "open",
                    "html_url": "https://github.com/YounghanKang/Templ/issues/17",
                    "updated_at": "2026-08-15T15:00:00Z"
                  },
                  "repository": {
                    "full_name": "YounghanKang/Templ"
                  },
                  "sender": {
                    "id": 12345,
                    "login": "octocat"
                  }
                }
                """;


        service.handleIssue(
                projectId,
                deliveryId,
                payload.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        ArgumentCaptor<String> contentCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );


        verify(
                ingestionService
        ).ingest(
                eq(projectId),
                eq(SourceTool.GITHUB),
                eq(CollaborationEventType.ISSUE),
                eq(EventAction.CREATED),
                eq(deliveryId),
                eq("GitHub issue #17: 로그인 API 구현 방향 변경"),
                contentCaptor.capture(),
                eq("12345"),
                eq("octocat"),
                eq("YounghanKang/Templ#17"),
                eq("https://github.com/YounghanKang/Templ/issues/17"),
                eq(false),
                eq(
                        Instant.parse(
                                "2026-08-15T15:00:00Z"
                        )
                )
        );


        String content =
                contentCaptor.getValue();


        assertTrue(
                content.contains(
                        "action: opened"
                )
        );

        assertTrue(
                content.contains(
                        "state: open"
                )
        );

        assertTrue(
                content.contains(
                        "JWT 대신 세션 인증 방식으로 변경하는 것을 제안합니다."
                )
        );
    }
    @Test
    void pullRequestOpenedEventIsConvertedAndIngested() {

        CollaborationEventIngestionService ingestionService =
                mock(
                        CollaborationEventIngestionService.class
                );


        JsonMapper jsonMapper =
                JsonMapper
                        .builder()
                        .build();


        GitHubWebhookService service =
                new GitHubWebhookService(
                        jsonMapper,
                        org.mockito.Mockito.mock(GitHubCompareClient.class),
                        ingestionService
                );


        UUID projectId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );

        String deliveryId =
                "github-delivery-pr-001";


        String payload =
                """
                {
                  "action": "opened",
                  "number": 25,
                  "pull_request": {
                    "title": "로그인 인증 구조 변경",
                    "body": "JWT 인증을 세션 인증으로 변경합니다.",
                    "state": "open",
                    "merged": false,
                    "draft": false,
                    "html_url": "https://github.com/YounghanKang/Templ/pull/25",
                    "updated_at": "2026-08-15T15:30:00Z",
                    "head": {
                      "ref": "feature/session-auth"
                    },
                    "base": {
                      "ref": "main"
                    }
                  },
                  "repository": {
                    "full_name": "YounghanKang/Templ"
                  },
                  "sender": {
                    "id": 12345,
                    "login": "octocat"
                  }
                }
                """;


        service.handlePullRequest(
                projectId,
                deliveryId,
                payload.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        ArgumentCaptor<String> contentCaptor =
                ArgumentCaptor.forClass(
                        String.class
                );


        verify(
                ingestionService
        ).ingest(
                eq(projectId),
                eq(SourceTool.GITHUB),
                eq(CollaborationEventType.PULL_REQUEST),
                eq(EventAction.CREATED),
                eq(deliveryId),
                eq("GitHub pull request #25: 로그인 인증 구조 변경"),
                contentCaptor.capture(),
                eq("12345"),
                eq("octocat"),
                eq("YounghanKang/Templ#25"),
                eq("https://github.com/YounghanKang/Templ/pull/25"),
                eq(false),
                eq(
                        Instant.parse(
                                "2026-08-15T15:30:00Z"
                        )
                )
        );


        String content =
                contentCaptor.getValue();


        assertTrue(
                content.contains(
                        "action: opened"
                )
        );

        assertTrue(
                content.contains(
                        "head: feature/session-auth"
                )
        );

        assertTrue(
                content.contains(
                        "base: main"
                )
        );

        assertTrue(
                content.contains(
                        "JWT 인증을 세션 인증으로 변경합니다."
                )
        );
    }
}