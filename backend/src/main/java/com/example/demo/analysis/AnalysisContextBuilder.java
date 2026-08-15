package com.example.demo.analysis;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.matching.CandidateNodeMatch;
import com.example.demo.matching.NodeMatchingResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisContextBuilder {

    public AnalysisCommand build(
            CollaborationEvent event,
            NodeMatchingResult matchingResult
    ) {

        if (event == null) {

            throw new IllegalArgumentException(
                    "event는 필수입니다."
            );
        }


        if (matchingResult == null) {

            throw new IllegalArgumentException(
                    "matchingResult는 필수입니다."
            );
        }


        List<AnalysisCandidate> candidates =
                matchingResult
                        .candidates()
                        .stream()
                        .map(
                                this::toAnalysisCandidate
                        )
                        .toList();


        return new AnalysisCommand(
                event.getProjectId(),
                event.getId(),
                event.getSourceTool().name(),
                event.getSourceLocation(),
                event.getTitle(),
                event.getContent(),
                event.getActorName(),
                candidates
        );
    }


    private AnalysisCandidate toAnalysisCandidate(
            CandidateNodeMatch match
    ) {

        return new AnalysisCandidate(
                match.nodeId(),
                match.title(),
                match.score(),
                match.matchedTerms()
        );
    }
}