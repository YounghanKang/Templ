package com.example.demo.analysis;

import com.example.demo.collaboration.CollaborationEvent;
import com.example.demo.matching.CandidateNodeMatch;
import com.example.demo.matching.NodeMatchingResult;
import com.example.demo.matching.ProjectContext;
import com.example.demo.matching.ProjectContextReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AnalysisContextBuilder {

    private final ProjectContextReader projectContextReader;


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


        ProjectContext projectContext =
                projectContextReader
                        .getProjectContext(
                                event.getProjectId()
                        );


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
                projectContext.projectName(),
                projectContext.projectSummary(),
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
                match.goal(),
                match.aiSummary(),
                match.status(),
                match.progress(),
                match.assignees(),
                match.prerequisites(),
                match.dueDate(),
                match.score(),
                match.matchedTerms()
        );
    }
}