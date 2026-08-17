package com.example.demo.matching;

import java.util.List;
import java.util.UUID;

public record NodeContext(
        String nodeId,
        String title,
        String goal,
        String summary,
        List<String> keywords
) {

    public NodeContext {

        keywords =
                keywords == null
                        ? List.of()
                        : List.copyOf(keywords);
    }
}