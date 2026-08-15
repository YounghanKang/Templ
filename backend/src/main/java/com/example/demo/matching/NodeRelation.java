package com.example.demo.matching;

import java.util.UUID;

public record NodeRelation(
        UUID fromNodeId,
        UUID toNodeId,
        String relationType
) {
}