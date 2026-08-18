package com.example.demo.matching;

public record NodeRelation(
        String fromNodeId,
        String toNodeId,
        String relationType
) {
}