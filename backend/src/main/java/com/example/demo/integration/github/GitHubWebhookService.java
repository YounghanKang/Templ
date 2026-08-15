package com.example.demo.integration.github;

import com.example.demo.collaboration.CollaborationEventIngestionService;
import com.example.demo.collaboration.CollaborationEventType;
import com.example.demo.collaboration.EventAction;
import com.example.demo.collaboration.SourceTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubWebhookService {

    private static final int MAX_COMMITS_IN_CONTENT = 20;

    private final JsonMapper jsonMapper;

    private final CollaborationEventIngestionService ingestionService;

    private EventAction resolvePullRequestAction(
            JsonNode root
    ) {

        String action =
                textOrNull(
                        root.path("action")
                );


        if (
                "opened".equals(
                        action
                )
        ) {
            return EventAction.CREATED;
        }


        return EventAction.UPDATED;
    }

    public CollaborationEventIngestionService.IngestionResult handlePush(
            UUID projectId,
            String deliveryId,
            byte[] payload
    ) {

        JsonNode root =
                parsePayload(
                        payload
                );


        EventAction eventAction =
                resolvePushAction(
                        root
                );


        String ref =
                textOrNull(
                        root.path("ref")
                );

        String repositoryName =
                textOrNull(
                        root.path("repository")
                                .path("full_name")
                );

        String actorId =
                scalarOrNull(
                        root.path("sender")
                                .path("id")
                );

        String actorName =
                textOrNull(
                        root.path("sender")
                                .path("login")
                );

        String sourceUrl =
                textOrNull(
                        root.path("compare")
                );


        String title =
                buildTitle(
                        ref
                );

        String content =
                buildContent(
                        root
                );

        String sourceLocation =
                buildSourceLocation(
                        repositoryName,
                        ref
                );

        Instant occurredAt =
                extractOccurredAt(
                        root
                );


        return ingestionService.ingest(
                projectId,
                SourceTool.GITHUB,
                CollaborationEventType.PUSH,
                eventAction,
                deliveryId,
                title,
                content,
                actorId,
                actorName,
                sourceLocation,
                sourceUrl,
                false,
                occurredAt
        );
    }

    public CollaborationEventIngestionService.IngestionResult handleIssue(
            UUID projectId,
            String deliveryId,
            byte[] payload
    ) {

        JsonNode root =
                parsePayload(
                        payload
                );

        JsonNode issue =
                root.path(
                        "issue"
                );


        EventAction eventAction =
                resolveIssueAction(
                        root
                );


        String action =
                textOrNull(
                        root.path("action")
                );

        String repositoryName =
                textOrNull(
                        root.path("repository")
                                .path("full_name")
                );

        String issueNumber =
                scalarOrNull(
                        issue.path("number")
                );

        String issueTitle =
                textOrNull(
                        issue.path("title")
                );

        String issueBody =
                textOrNull(
                        issue.path("body")
                );

        String issueState =
                textOrNull(
                        issue.path("state")
                );

        String actorId =
                scalarOrNull(
                        root.path("sender")
                                .path("id")
                );

        String actorName =
                textOrNull(
                        root.path("sender")
                                .path("login")
                );

        String sourceUrl =
                textOrNull(
                        issue.path("html_url")
                );


        String title =
                "GitHub issue"
                        + (
                        issueNumber == null
                                ? ""
                                : " #" + issueNumber
                )
                        + (
                        issueTitle == null
                                ? ""
                                : ": " + issueTitle
                );


        StringBuilder content =
                new StringBuilder();

        appendLine(
                content,
                "action",
                action
        );

        appendLine(
                content,
                "state",
                issueState
        );

        appendLine(
                content,
                "body",
                issueBody
        );


        String sourceLocation =
                repositoryName;

        if (
                repositoryName != null
                        &&
                        issueNumber != null
        ) {

            sourceLocation =
                    repositoryName
                            + "#"
                            + issueNumber;
        }


        Instant occurredAt =
                parseInstantOrNull(
                        textOrNull(
                                issue.path(
                                        "updated_at"
                                )
                        )
                );


        return ingestionService.ingest(
                projectId,
                SourceTool.GITHUB,
                CollaborationEventType.ISSUE,
                eventAction,
                deliveryId,
                title,
                content.toString().trim(),
                actorId,
                actorName,
                sourceLocation,
                sourceUrl,
                false,
                occurredAt
        );
    }
    public CollaborationEventIngestionService.IngestionResult handlePullRequest(
            UUID projectId,
            String deliveryId,
            byte[] payload
    ) {

        JsonNode root =
                parsePayload(
                        payload
                );

        JsonNode pullRequest =
                root.path(
                        "pull_request"
                );


        EventAction eventAction =
                resolvePullRequestAction(
                        root
                );


        String action =
                textOrNull(
                        root.path("action")
                );

        String repositoryName =
                textOrNull(
                        root.path("repository")
                                .path("full_name")
                );

        String pullRequestNumber =
                scalarOrNull(
                        root.path("number")
                );

        String pullRequestTitle =
                textOrNull(
                        pullRequest.path("title")
                );

        String pullRequestBody =
                textOrNull(
                        pullRequest.path("body")
                );

        String pullRequestState =
                textOrNull(
                        pullRequest.path("state")
                );

        String headRef =
                textOrNull(
                        pullRequest.path("head")
                                .path("ref")
                );

        String baseRef =
                textOrNull(
                        pullRequest.path("base")
                                .path("ref")
                );

        String merged =
                scalarOrNull(
                        pullRequest.path("merged")
                );

        String draft =
                scalarOrNull(
                        pullRequest.path("draft")
                );

        String actorId =
                scalarOrNull(
                        root.path("sender")
                                .path("id")
                );

        String actorName =
                textOrNull(
                        root.path("sender")
                                .path("login")
                );

        String sourceUrl =
                textOrNull(
                        pullRequest.path("html_url")
                );


        String title =
                "GitHub pull request"
                        + (
                        pullRequestNumber == null
                                ? ""
                                : " #" + pullRequestNumber
                )
                        + (
                        pullRequestTitle == null
                                ? ""
                                : ": " + pullRequestTitle
                );


        StringBuilder content =
                new StringBuilder();


        appendLine(
                content,
                "action",
                action
        );

        appendLine(
                content,
                "state",
                pullRequestState
        );

        appendLine(
                content,
                "head",
                headRef
        );

        appendLine(
                content,
                "base",
                baseRef
        );

        appendLine(
                content,
                "merged",
                merged
        );

        appendLine(
                content,
                "draft",
                draft
        );

        appendLine(
                content,
                "body",
                pullRequestBody
        );


        String sourceLocation =
                repositoryName;

        if (
                repositoryName != null
                        &&
                        pullRequestNumber != null
        ) {

            sourceLocation =
                    repositoryName
                            + "#"
                            + pullRequestNumber;
        }


        Instant occurredAt =
                parseInstantOrNull(
                        textOrNull(
                                pullRequest.path(
                                        "updated_at"
                                )
                        )
                );


        return ingestionService.ingest(
                projectId,
                SourceTool.GITHUB,
                CollaborationEventType.PULL_REQUEST,
                eventAction,
                deliveryId,
                title,
                content.toString().trim(),
                actorId,
                actorName,
                sourceLocation,
                sourceUrl,
                false,
                occurredAt
        );
    }


    private JsonNode parsePayload(
            byte[] payload
    ) {

        if (
                payload == null
                        ||
                        payload.length == 0
        ) {

            throw new IllegalArgumentException(
                    "GitHub Webhook payload는 필수입니다."
            );
        }


        try {

            return jsonMapper.readTree(
                    payload
            );

        } catch (JacksonException exception) {

            throw new IllegalArgumentException(
                    "GitHub Webhook payload JSON 파싱에 실패했습니다.",
                    exception
            );
        }
    }


    private EventAction resolvePushAction(
            JsonNode root
    ) {

        if (
                root.path("created")
                        .asBoolean(false)
        ) {
            return EventAction.CREATED;
        }


        if (
                root.path("deleted")
                        .asBoolean(false)
        ) {
            return EventAction.DELETED;
        }


        return EventAction.UPDATED;
    }


    private String buildTitle(
            String ref
    ) {

        if (
                ref == null
                        ||
                        ref.isBlank()
        ) {
            return "GitHub push";
        }


        return "GitHub push: "
                + ref;
    }


    private String buildContent(
            JsonNode root
    ) {

        StringBuilder content =
                new StringBuilder();


        appendLine(
                content,
                "ref",
                textOrNull(
                        root.path("ref")
                )
        );

        appendLine(
                content,
                "before",
                textOrNull(
                        root.path("before")
                )
        );

        appendLine(
                content,
                "after",
                textOrNull(
                        root.path("after")
                )
        );

        appendLine(
                content,
                "forced",
                scalarOrNull(
                        root.path("forced")
                )
        );


        JsonNode commits =
                root.path("commits");


        if (
                commits.isArray()
                        &&
                        !commits.isEmpty()
        ) {

            content.append(
                    "commits:"
            );

            content.append(
                    System.lineSeparator()
            );


            int count = 0;


            for (JsonNode commit : commits) {

                if (
                        count
                                >= MAX_COMMITS_IN_CONTENT
                ) {

                    content.append(
                            "- ... additional commits omitted"
                    );

                    content.append(
                            System.lineSeparator()
                    );

                    break;
                }


                String commitId =
                        textOrNull(
                                commit.path("id")
                        );

                String message =
                        textOrNull(
                                commit.path("message")
                        );

                content.append(
                        "- "
                );

                if (
                        commitId != null
                                &&
                                !commitId.isBlank()
                ) {

                    content.append(
                            shortenCommitId(
                                    commitId
                            )
                    );

                    content.append(
                            " "
                    );
                }


                if (message != null) {

                    content.append(
                            message
                                    .replace(
                                            "\r",
                                            " "
                                    )
                                    .replace(
                                            "\n",
                                            " "
                                    )
                    );
                }


                content.append(
                        System.lineSeparator()
                );

                count++;
            }
        }


        return content.toString()
                .trim();
    }


    private String buildSourceLocation(
            String repositoryName,
            String ref
    ) {

        if (
                repositoryName == null
                        ||
                        repositoryName.isBlank()
        ) {
            return ref;
        }


        if (
                ref == null
                        ||
                        ref.isBlank()
        ) {
            return repositoryName;
        }


        return repositoryName
                + ":"
                + ref;
    }


    private Instant extractOccurredAt(
            JsonNode root
    ) {

        String timestamp =
                textOrNull(
                        root.path("head_commit")
                                .path("timestamp")
                );


        if (
                timestamp == null
                        ||
                        timestamp.isBlank()
        ) {
            return null;
        }


        try {

            return Instant.parse(
                    timestamp
            );

        } catch (DateTimeParseException exception) {

            return null;
        }
    }


    private void appendLine(
            StringBuilder builder,
            String name,
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {
            return;
        }


        builder.append(
                name
        );

        builder.append(
                ": "
        );

        builder.append(
                value
        );

        builder.append(
                System.lineSeparator()
        );
    }


    private String shortenCommitId(
            String commitId
    ) {

        if (
                commitId.length()
                        <= 7
        ) {
            return commitId;
        }


        return commitId.substring(
                0,
                7
        );
    }


    private String textOrNull(
            JsonNode node
    ) {

        if (
                node == null
                        ||
                        node.isMissingNode()
                        ||
                        node.isNull()
                        ||
                        !node.isString()
        ) {
            return null;
        }


        return node.stringValue();
    }


    private String scalarOrNull(
            JsonNode node
    ) {

        if (
                node == null
                        ||
                        node.isMissingNode()
                        ||
                        node.isNull()
                        ||
                        node.isContainer()
        ) {
            return null;
        }


        return node.asString();
    }

    private EventAction resolveIssueAction(
            JsonNode root
    ) {

        String action =
                textOrNull(
                        root.path("action")
                );


        if (
                "opened".equals(
                        action
                )
        ) {
            return EventAction.CREATED;
        }


        if (
                "deleted".equals(
                        action
                )
        ) {
            return EventAction.DELETED;
        }


        return EventAction.UPDATED;
    }
    private Instant parseInstantOrNull(
            String timestamp
    ) {

        if (
                timestamp == null
                        ||
                        timestamp.isBlank()
        ) {
            return null;
        }


        try {

            return Instant.parse(
                    timestamp
            );

        } catch (DateTimeParseException exception) {

            return null;
        }
    }
}