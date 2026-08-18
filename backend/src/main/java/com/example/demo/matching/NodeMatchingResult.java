package com.example.demo.matching;

import java.util.List;

public record NodeMatchingResult(
        String projectId,
        String channelId,
        List<CandidateNodeMatch> candidates,
        boolean mapped
) {

    public NodeMatchingResult {

        candidates =
                candidates == null
                        ? List.of()
                        : List.copyOf(candidates);
    }


    public static NodeMatchingResult mapped(
            String projectId,
            String channelId,
            List<CandidateNodeMatch> candidates
    ) {

        return new NodeMatchingResult(
                projectId,
                channelId,
                candidates,
                true
        );
    }


    public static NodeMatchingResult unmapped(
            String projectId,
            String channelId
    ) {

        return new NodeMatchingResult(
                projectId,
                channelId,
                List.of(),
                false
        );
    }
}