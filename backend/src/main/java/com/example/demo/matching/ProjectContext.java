package com.example.demo.matching;

import java.util.List;
import java.util.UUID;

public record ProjectContext(
        String projectId,
        String projectName,
        String projectSummary,
        long projectVersion,
        List<NodeContext> nodes
) {

    public ProjectContext {

        nodes =
                nodes == null
                        ? List.of()
                        : List.copyOf(nodes);
    }
}