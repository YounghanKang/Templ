package com.example.demo.matching;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class NodeMatchingService {

    private static final int MAX_CANDIDATES = 5;

    private final ProjectContextReader projectContextReader;

    private final CandidateNodeMatcher candidateNodeMatcher;

    public NodeMatchingResult match(
            String projectId,
            String channelId,
            String eventText
    ) {

        /*
         * 1. 기본 입력 검증
         */
        if (projectId == null) {

            throw new IllegalArgumentException(
                    "projectId는 필수입니다."
            );
        }


        if (
                eventText == null
                        || eventText.isBlank()
        ) {

            return NodeMatchingResult.unmapped(
                    projectId,
                    channelId
            );
        }


        /*
         * 2. Backend1에게 1차 후보 Node 요청
         *
         * Backend2가 ProjectNode DB를
         * 직접 조회하면 안 됩니다.
         */
        List<NodeContext> candidates =
                projectContextReader
                        .findCandidateNodes(
                                projectId,
                                channelId
                        );


        /*
         * 3. 후보 자체가 없다면 UNMAPPED
         */
        if (
                candidates == null
                        || candidates.isEmpty()
        ) {

            return NodeMatchingResult.unmapped(
                    projectId,
                    channelId
            );
        }


        /*
         * 4. Backend2 규칙 기반 Top 5 정렬
         */
        List<CandidateNodeMatch> ranked =
                candidateNodeMatcher
                        .findTopCandidates(
                                eventText,
                                candidates,
                                MAX_CANDIDATES
                        );


        /*
         * 5. 관련 후보가 하나도 없으면 UNMAPPED
         */
        if (ranked.isEmpty()) {

            return NodeMatchingResult.unmapped(
                    projectId,
                    channelId
            );
        }


        return NodeMatchingResult.mapped(
                projectId,
                channelId,
                ranked
        );
    }
}