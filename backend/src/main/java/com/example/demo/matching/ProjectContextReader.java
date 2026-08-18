package com.example.demo.matching;

import java.util.List;

public interface ProjectContextReader {

    ProjectContext getProjectContext(
            String projectId
    );

    List<NodeContext> findCandidateNodes(
            String projectId,
            String channelId
    );

    List<NodeRelation> getRelatedNodes(
            String projectId,
            String nodeId
    );
}