package com.example.demo.matching;

import java.util.UUID;

public record NodeRelation(
        String fromNodeId,
        String toNodeId,
        String relationType
) {
}