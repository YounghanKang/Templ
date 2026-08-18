package com.example.demo.integration.github;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GitHubCompareClient {

    private static final String GITHUB_API_BASE_URL =
            "https://api.github.com";

    private static final String GITHUB_API_VERSION =
            "2026-03-10";

    private final RestClient restClient;


    public GitHubCompareClient() {

        this(
                RestClient.builder()
        );
    }


    GitHubCompareClient(
            RestClient.Builder restClientBuilder
    ) {

        this.restClient =
                restClientBuilder
                        .baseUrl(
                                GITHUB_API_BASE_URL
                        )
                        .defaultHeader(
                                HttpHeaders.ACCEPT,
                                "application/vnd.github+json"
                        )
                        .defaultHeader(
                                "X-GitHub-Api-Version",
                                GITHUB_API_VERSION
                        )
                        .defaultHeader(
                                HttpHeaders.USER_AGENT,
                                "TEMPL-backend2"
                        )
                        .build();
    }


    public CompareResult compare(
            String repositoryFullName,
            String before,
            String after
    ) {

        if (
                repositoryFullName == null
                        ||
                        repositoryFullName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "repositoryFullName은 필수입니다."
            );
        }


        if (
                before == null
                        ||
                        before.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "before SHA는 필수입니다."
            );
        }


        if (
                after == null
                        ||
                        after.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "after SHA는 필수입니다."
            );
        }


        String[] repositoryParts =
                repositoryFullName.split(
                        "/",
                        2
                );


        if (
                repositoryParts.length != 2
                        ||
                        repositoryParts[0].isBlank()
                        ||
                        repositoryParts[1].isBlank()
        ) {
            throw new IllegalArgumentException(
                    "repositoryFullName은 owner/repository 형식이어야 합니다."
            );
        }


        String owner =
                repositoryParts[0];

        String repository =
                repositoryParts[1];

        String baseHead =
                before
                        + "..."
                        + after;


        CompareResponse response =
                restClient
                        .get()
                        .uri(
                                "/repos/{owner}/{repo}/compare/{baseHead}",
                                owner,
                                repository,
                                baseHead
                        )
                        .retrieve()
                        .body(
                                CompareResponse.class
                        );


        if (
                response == null
                        ||
                        response.files == null
        ) {
            return new CompareResult(
                    List.of()
            );
        }


        List<ChangedFile> files =
                response.files
                        .stream()
                        .map(
                                file ->
                                        new ChangedFile(
                                                file.filename,
                                                file.status,
                                                file.additions,
                                                file.deletions,
                                                file.changes,
                                                file.patch
                                        )
                        )
                        .toList();


        return new CompareResult(
                files
        );
    }


    public record CompareResult(
            List<ChangedFile> files
    ) {

        public CompareResult {

            files =
                    files == null
                            ? List.of()
                            : List.copyOf(
                            files
                    );
        }
    }


    public record ChangedFile(
            String filename,
            String status,
            int additions,
            int deletions,
            int changes,
            String patch
    ) {
    }


    private static class CompareResponse {

        public List<CompareFileResponse> files;
    }


    private static class CompareFileResponse {

        public String filename;

        public String status;

        public int additions;

        public int deletions;

        public int changes;

        public String patch;
    }
}