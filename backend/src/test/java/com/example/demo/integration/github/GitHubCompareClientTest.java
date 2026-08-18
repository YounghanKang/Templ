package com.example.demo.integration.github;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubCompareClientTest {

    @Test
    void compareReturnsChangedFiles() {

        RestClient.Builder builder =
                RestClient.builder();


        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();


        GitHubCompareClient client =
                new GitHubCompareClient(
                        builder
                );


        server.expect(
                        requestTo(
                                "https://api.github.com/repos/YounghanKang/Templ/compare/aaaaaaaa...bbbbbbbb"
                        )
                )
                .andExpect(
                        method(
                                HttpMethod.GET
                        )
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "files": [
                                    {
                                      "filename": "src/main/java/com/example/demo/AuthService.java",
                                      "status": "modified",
                                      "additions": 12,
                                      "deletions": 3,
                                      "changes": 15,
                                      "patch": "@@ -1 +1 @@\\n-old auth\\n+new auth"
                                    }
                                  ]
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );


        GitHubCompareClient.CompareResult result =
                client.compare(
                        "YounghanKang/Templ",
                        "aaaaaaaa",
                        "bbbbbbbb"
                );


        assertEquals(
                1,
                result.files().size()
        );


        GitHubCompareClient.ChangedFile file =
                result.files()
                        .get(0);


        assertEquals(
                "src/main/java/com/example/demo/AuthService.java",
                file.filename()
        );

        assertEquals(
                "modified",
                file.status()
        );

        assertEquals(
                12,
                file.additions()
        );

        assertEquals(
                3,
                file.deletions()
        );

        assertEquals(
                15,
                file.changes()
        );

        assertEquals(
                "@@ -1 +1 @@\n-old auth\n+new auth",
                file.patch()
        );


        server.verify();
    }
}